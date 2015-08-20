/*
 * Copyright (c) 2001 by Matt Welsh and The Regents of the University of 
 * California. All rights reserved.
 *
 * Permission to use, copy, modify, and distribute this software and its
 * documentation for any purpose, without fee, and without written agreement is
 * hereby granted, provided that the above copyright notice and the following
 * two paragraphs appear in all copies of this software.
 * 
 * IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY FOR
 * DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES ARISING OUT
 * OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF THE UNIVERSITY OF
 * CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 * AND FITNESS FOR A PARTICULAR PURPOSE.  THE SOFTWARE PROVIDED HEREUNDER IS
 * ON AN "AS IS" BASIS, AND THE UNIVERSITY OF CALIFORNIA HAS NO OBLIGATION TO
 * PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS.
 *
 * Author: Matt Welsh <mdw@cs.berkeley.edu>
 * 
 */

package org.jcyclone.core.internal;

import org.jcyclone.core.cfg.ConfigData;
import org.jcyclone.core.cfg.ConfigDataProxy;
import org.jcyclone.core.cfg.IConfigData;
import org.jcyclone.core.cfg.ISystemConfig;
import org.jcyclone.core.handler.IEventHandler;
import org.jcyclone.core.profiler.IProfilable;
import org.jcyclone.core.queue.*;
import org.jcyclone.core.rtc.*;
import org.jcyclone.core.stage.IStage;
import org.jcyclone.core.stage.IStageManager;
import org.jcyclone.core.stage.Stage;

import java.util.ArrayList;
import java.util.List;

/**
 * A StageWrapper is a basic implementation of IStageWrapper for
 * application-level stages.
 *
 * @author Matt Welsh and Jean Morissette
 */
public class StageWrapper implements IStageWrapper {

	private IStageManager mgr;
	private ISystemManager sysmgr;
	private String name;
	private IStage stage;
	private IEventHandler handler;
	private IConfigData config;
	private IBlockingQueue eventQ;
	private AdmissionControlledSink admContSink;
	private IScheduler threadmgr;
	private IStageStats stats;
	private IResponseTimeController rtc;
	private IBatchSorter sorter;
	private int status; // lifecycle level
	private boolean reprogrammable;

	/**
	 * Create a StageWrapper with the given name and config data.
	 */
	public StageWrapper(ISystemManager sysmgr, IStageManager mgr, String name,
	                    IEventHandler handler, String[] initargs) throws Exception {
		this.sysmgr = sysmgr;
		this.mgr = mgr;
		this.name = name;
		this.handler = handler;
		this.config = new ConfigData(mgr, initargs);
		this.reprogrammable = false;
		status = LOADED;
	}

	/**
	 * Create a StageWrapper with the given name and config data.
	 */
	public StageWrapper(ISystemManager sysmgr, IStageManager mgr, String name) {
		this.sysmgr = sysmgr;
		this.mgr = mgr;
		this.name = name;
		// XXX JM: throw an exeption if IGlobalConfig does not contain the stagename?
		this.config = new ConfigDataProxy(mgr, name);
		this.reprogrammable = true;
		status = LOADED;
	}

	public void program() throws Exception {
		if (status >= PROGRAMMED) return;

		System.err.print("Program Stage <" + name + ">");

		ISystemConfig mgrcfg = mgr.getConfig();
		String tag = "stages." + name + ".";

		String tmname = mgrcfg.getString(tag + "threadManager");
		this.threadmgr = sysmgr.getScheduler(tmname);
		if (this.threadmgr == null)
			this.threadmgr = sysmgr.getScheduler();

		if (reprogrammable) {
			// XXX JM: change the handler only if it's not the same implementation
			String classname = mgrcfg.getString(tag + "class");
			try {
				Class theclass = Class.forName(classname);
				this.handler = (IEventHandler) theclass.newInstance();
			} catch (ClassNotFoundException cnfe) {
				System.err.println("The class '" + classname + "' cannot be located");
				throw cnfe;
			}
		}

		// XXX JM: queue implementation should be specified in the config
		if (this.eventQ == null)
//			this.eventQ = new LinkedBlockingQueue();
			this.eventQ = new DynamicArrayBlockingQueue();
		else {
			// XXX JM: change the queue only if it's not the same implementation
			// create the new queue
			IQueue newQueue = new LinkedBlockingQueue();
			try {
				// ensure that the new queue has enough space
				int size = this.eventQ.size();
				if (newQueue.capacity() < size) {
					newQueue.setCapacity(size);
				}
				// transfer remaining events into the new queue
				List buffer = new ArrayList();
				this.eventQ.dequeueAll(buffer);
				newQueue.enqueueMany(buffer);
			} catch (SinkException e) {
				e.printStackTrace();
			}
		}

		int queueThreshold = mgrcfg.getInt(tag + "queueThreshold", -1);
		IEnqueuePredicate pred = new QueueThresholdPredicate(eventQ, queueThreshold);
		admContSink = new AdmissionControlledSink(eventQ);
		admContSink.setEnqueuePredicate(pred);

		if (mgrcfg.getBoolean("global.batchController.enable")) {
			System.err.print(", batch controller enabled");
			this.sorter = new AggThrottleBatchSorter();
		} else {
			this.sorter = new NullBatchSorter();
		}

		this.stats = new StageStats(this);
		this.stage = new Stage(name, this, (ISink) admContSink, config);

		// XXX JM: I know, this is ugly
		if (config instanceof ConfigData) {
			((ConfigData) config).setStage(this.stage);
		}

		ISystemConfig sysConfig = mgr.getConfig();
		boolean rtControllerEnabled = sysConfig.getBoolean("global.rtController.enable");
		String defType = sysConfig.getString("global.rtController.type");

		rtControllerEnabled = sysConfig.getBoolean(tag + "rtController.enable", rtControllerEnabled);
		String contype = sysConfig.getString(tag + "rtController.type", defType);

		// override from stage config
		if (config.contains("rtController.enable")) {
			rtControllerEnabled = config.getBoolean("rtController.enable");
		}

		if (config.contains("rtController.type")) {
			contype = config.getString("rtController.type");
		}

		if (rtControllerEnabled) {
			if (contype == null) {
				System.err.print("direct");
				this.rtc = new ResponseTimeControllerDirect(mgr, this);
			} else if (contype.equals("direct")) {
				System.err.print("direct");
				this.rtc = new ResponseTimeControllerDirect(mgr, this);
			} else if (contype.equals("mm1")) {
				System.err.print("mm1");
				this.rtc = new ResponseTimeControllerMM1(mgr, this);
			} else if (contype.equals("pid")) {
				System.err.print("pid");
				this.rtc = new ResponseTimeControllerPID(mgr, this);
			} else if (contype.equals("multiclass")) {
				System.err.print("multiclass");
				this.rtc = new ResponseTimeControllerMulticlass(mgr, this);
			} else {
				throw new RuntimeException("StageWrapper <" + name + ">: Bad response time controller type " + contype);
			}
		}

		if (mgrcfg.getBoolean("global.profile.enable")) {
			mgr.getProfiler().add(name + " queueLength",
			    (IProfilable) stage.getSink());
		}
		status = PROGRAMMED;
	}

	/**
	 * Initialize this stage.
	 */
	public void init() throws Exception {
		if (status >= INITIALIZED) return;
		program();
		System.err.println("-- Initializing <" + name + ">");
		handler.init(config);
		status = INITIALIZED;
	}

	public void start() throws Exception {
		if (status >= STARTED) return;
		init();
		System.err.println("-- Starting <" + name + ">");
		threadmgr.register(this);
		status = STARTED;
	}

	public void stop() {
		if (status <= INITIALIZED) return;
		System.err.println("-- Stopping <" + name + ">");
		threadmgr.deregister(this);
		status = INITIALIZED;
	}

	/**
	 * Destroy this stage.
	 */
	public void destroy() throws Exception {
		if (status <= PROGRAMMED) return;
		stop();
		System.err.println("-- Destroying <" + name + ">");
		handler.destroy();
		status = PROGRAMMED;
	}

	public void deprogram() throws Exception {
		if (status <= LOADED) return;
		destroy();
		this.mgr.getProfiler().remove(name + " queueLength");
		status = LOADED;
	}

	public int getLifecycleLevel() {
		return status;
	}

	/**
	 * Return the event handler associated with this stage.
	 */
	public IEventHandler getEventHandler() {
		return handler;
	}

	/**
	 * Return the stage handle for this stage.
	 */
	public IStage getStage() {
		return stage;
	}

	public String getName() {
		return name;
	}

	/**
	 * Return the set of sources from which events should be pulled to
	 * pass to this IEventHandler.
	 */
	public ISource getSource() {
		return (ISource) eventQ;
	}

	/**
	 * Return the thread manager which will run this stage.
	 */
	public IScheduler getThreadManager() {
		return threadmgr;
	}

	/**
	 * Return execution statistics for this stage.
	 */
	public IStageStats getStats() {
		return stats;
	}

	/**
	 * Return the response time controller, if any.
	 */
	public IResponseTimeController getResponseTimeController() {
		return rtc;
	}

	/**
	 * Set the batch sorter.
	 */
	public void setBatchSorter(IBatchSorter sorter) {
		this.sorter = sorter;
	}

	public IAdmissionControlledSink getSink() {
		return admContSink;
	}

	/**
	 * Return the batch sorter.
	 */
	public IBatchSorter getBatchSorter() {
		return sorter;
	}

	public String toString() {
		return "SW[" + stage.getName() + "]";
	}

}

