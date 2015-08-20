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

package org.jcyclone.core.stage;

import org.jcyclone.core.cfg.ISystemConfig;
import org.jcyclone.core.cfg.JCycloneConfig;
import org.jcyclone.core.handler.IEventHandler;
import org.jcyclone.core.internal.*;
import org.jcyclone.core.profiler.IProfiler;
import org.jcyclone.core.profiler.JCycloneProfiler;
import org.jcyclone.core.signal.ISignalMgr;
import org.jcyclone.core.signal.JCycloneSignalMgr;
import org.jcyclone.core.signal.StagesInitializedSignal;
import org.jcyclone.core.timer.ITimer;
import org.jcyclone.core.timer.JCycloneTimer;
import org.jcyclone.core.plugin.IPlugin;

import java.util.*;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class provides management functionality for the JCyclone
 * runtime system. It is responsible for initializing the system,
 * creating and registering stages and thread managers, and other
 * administrative functions. Stages and thread managers can interact
 * with this class through the ManagerIF and SystemManagerIF interfaces;
 * this class should not be used directly.
 *
 * @author Matt Welsh and Jean Morissette
 * @see org.jcyclone.core.stage.IStageManager
 * @see org.jcyclone.core.internal.ISystemManager
 */
public class JCycloneMgr implements IStageManager, ISystemManager {

	private IScheduler defaulttm;
	private Map tmtbl;

	private ISystemConfig mgrconfig;
	private ConcurrentMap stagetbl;    // stage name --> StageWrapper
	private JCycloneProfiler profiler;
	private JCycloneSignalMgr signalMgr;
	private JCycloneTimer timer;
	private boolean crashOnException = false;

	/**
	 * Create a JCycloneMgr which reads its configuration from the
	 * given file.
	 */
	public JCycloneMgr(ISystemConfig mgrconfig) throws Exception {
		this.mgrconfig = mgrconfig;

		stagetbl = new ConcurrentHashMap();
		tmtbl = Collections.synchronizedMap(new HashMap());
		signalMgr = new JCycloneSignalMgr(mgrconfig);
		timer = new JCycloneTimer();

		crashOnException = mgrconfig.getBoolean("global.crashOnException");
		String dtm = mgrconfig.getString("global.defaultThreadManager");
		if (dtm == null) {
			throw new IllegalArgumentException("No threadmanager specified by configuration");
		}


//		defaulttm = new ExperimentalRRScheduler();

		if (dtm.equals(JCycloneConfig.THREADMGR_TPPTM)) {
			throw new Error("TPPThreadManager is no longer supported.");
//			defaulttm = new TPPSchedulerOld(mgrconfig);
		} else if (dtm.equals(JCycloneConfig.THREADMGR_TPSTM)) {
			defaulttm = new TPSScheduler(this);
		} else if (dtm.equals(JCycloneConfig.THREADMGR_AggTPSTM)) {
			throw new Error("AggTPSThreadManager is no longer supported.");
//			 defaulttm = new AggTPSThreadManager(mgrconfig);
		} else {
			throw new IllegalArgumentException("Bad threadmanager specified by configuration: " + dtm);
		}

		tmtbl.put("default", defaulttm);

		// Create profiler even if disabled
		profiler = new JCycloneProfiler(this);

		if (mgrconfig.getBoolean("global.profile.enable")) {
			System.err.println("JCyclone: Starting profiler");
			profiler.start();
		}

		initializePlugins();
//		loadStages();
	}

	private void initializePlugins() throws Exception {

		String[] exts = ((JCycloneConfig)mgrconfig).getPluginNames();
		for (int i = 0; i < exts.length; i++) {
			try {
				String className = mgrconfig.getString("plugins." + exts[i] + ".class");
				Class cls = Class.forName(className);
				IPlugin plugin = (IPlugin) cls.newInstance();
				plugin.initialize((IStageManager)this, (ISystemManager)this);
			} catch (Throwable t) {
				System.err.println(t.getMessage());
			}
		}

//		if (mgrconfig.getBoolean("global.aSocket.enable")) {
//			System.err.println("JCyclone: Starting aSocket layer");
//			// TODO: JCyclone must not be dependant on aSocketMgr - see IoC principle
//			ASocketMgr.initialize(this, this);
//
//		}
//
//		if (mgrconfig.getBoolean("global.aDisk.enable")) {
//			System.err.println("JCyclone: Starting aDisk layer");
//			// TODO: JCyclone must not be dependant on AFileMgr - see IoC principle
//			AFileMgr.initialize(this, this);
//		}
	}

	void stop() throws Exception {
		unloadStages();
		profiler.stop();
		signalMgr.stop();
	}

	/**
	 * Return a handle to given stage.
	 */
	public IStage getStage(String stagename) throws NoSuchStageException {
		return getWrapper(stagename).getStage();
	}

	/**
	 * Return the default thread manager.
	 */
	public IScheduler getScheduler() {
		return defaulttm;
	}

	/**
	 * Return the thread manager with the given name.
	 */
	public IScheduler getScheduler(String name) {
		return (IScheduler) tmtbl.get(name);
	}

	/**
	 * Add a thread manager with the given name.
	 */
	public void addScheduler(String name, IScheduler tm) {
		// XXX JM: I think that we should not replace the tm if the given name is already used
		// because it would not be possible to deregister stages from the replaced tm.
		tmtbl.put(name, tm);
	}

	/**
	 * Create and start a stage with the given name from the given event handler with
	 * the given initial arguments.
	 */
	public IStage createStage(String stageName, IEventHandler evHandler,
	                          String[] initargs) throws StageNameAlreadyBoundException, Exception {
		// XXX JM: Actually, a stage created with this method cannot be reloaded
		// because the config is lost on the unload!

		if (stagetbl.containsKey(stageName)) {
			throw new StageNameAlreadyBoundException("Stage name " + stageName + " already in use");
		}

		IStageWrapper wrapper = new StageWrapper((ISystemManager) this, (IStageManager) this, stageName, evHandler, initargs);
		return createStage(wrapper, true);
	}

	/**
	 * Create a stage from the given stage wrapper.
	 * If 'start' is true, start this stage immediately.
	 */
	public IStage createStage(IStageWrapper wrapper, boolean start) throws Exception {

		// XXX JM: Actually, a stage created with this method cannot be reloaded
		// because the config is lost on the unload!

		// XXX JM: an error will occur if the given wrapper is not already programmed and
		// system config don't have information for this stage.

		String name = wrapper.getName();
		if (stagetbl.get(name) != null) {
			throw new StageNameAlreadyBoundException("Stage name " + name + " already in use");
		}
		stagetbl.put(name, wrapper);

		if (start) {
			try {
				startStage(name);
			} catch (Exception e) {
				System.err.println("JCyclone: Got exception starting stage " + name);
				e.printStackTrace();
				if (crashOnException) {
					System.err.println("JCyclone: Crashing runtime due to exception - goodbye");
					System.exit(-1);
				}
				throw e;
			}
		}
		return wrapper.getStage();
	}

	/**
	 * Return the system profiler.
	 */
	public IProfiler getProfiler() {
		return profiler;
	}

	public ITimer getTimer() {
		return timer;
	}

	/**
	 * Return the system signal manager.
	 */
	public ISignalMgr getSignalMgr() {
		return signalMgr;
	}

	/**
	 * Return the ISystemConfig used to initialize this manager.
	 * Actually returns an immutable ISystemConfig; this prevents
	 * options from being changed.
	 */
	public ISystemConfig getConfig() {
		return mgrconfig;
	}

	public void loadStage(String stagename) throws StageNameAlreadyBoundException, NoSuchStageException {
		if (stagename != null && stagetbl.get(stagename) != null) {
			throw new StageNameAlreadyBoundException("Stage name " + stagename + " already in use");
		}
		loadWrapper(stagename);
	}

	public void programStage(String stagename) throws Exception {
		loadWrapper(stagename).program();
	}

	public void initStage(String stagename) throws Exception {
		loadWrapper(stagename).init();
	}

	public void startStage(String stagename) throws Exception {
		loadWrapper(stagename).start();
	}

	public void stopStage(String stagename) throws Exception {
		getWrapper(stagename).stop();
	}

	public void destroyStage(String stagename) throws Exception {
		getWrapper(stagename).destroy();
	}

	public void deprogramStage(String stagename) throws Exception {
		getWrapper(stagename).deprogram();
	}

	public void unloadStage(String stagename) throws Exception {
		IStageWrapper wrapper = getWrapper(stagename);
		wrapper.deprogram();
		System.err.println("-- Unloading <" + stagename + ">");
		// remove from all data structures
		stagetbl.remove(stagename);
	}

	// Load stages as specified in the ISystemConfig.
	public void loadStages() {
		System.err.println("JCyclone: Loading stages");
		String[] names = mgrconfig.getStageNames();
		for (int i = 0; i < names.length; i++) {
			try {
				loadWrapper(names[i]);
			} catch (Exception ex) {
				System.err.println("JCyclone: Caught exception loading stage "
				    + names[i] + ": " + ex);
				ex.printStackTrace();
				if (crashOnException) {
					System.err.println("JCyclone: Crashing runtime due to exception - goodbye");
					System.exit(-1);
				}
			}
		}
	}

	public void programStages() {
		loadStages();
		System.err.println("JCyclone: Programming stages");
		Iterator it = stagetbl.values().iterator();
		while (it.hasNext()) {
			IStageWrapper wrapper = (IStageWrapper) it.next();
			try {
				wrapper.program();
			} catch (Exception ex) {
				System.err.println("JCyclone: Caught exception programming stage "
				    + wrapper.getStage().getName() + ": " + ex);
				ex.printStackTrace();
				if (crashOnException) {
					System.err.println("JCyclone: Crashing runtime due to exception - goodbye");
					System.exit(-1);
				}
			}
		}
	}

	// Initialize all stages
	public void initStages() {
		programStages();
		System.err.println("JCyclone: Initializing stages");
		Iterator it = stagetbl.values().iterator();
		while (it.hasNext()) {
			IStageWrapper wrapper = (IStageWrapper) it.next();
			try {
				wrapper.init();
			} catch (Exception ex) {
				System.err.println("JCyclone: Caught exception initializing stage "
				    + wrapper.getStage().getName() + ": " + ex);
				ex.printStackTrace();
				if (crashOnException) {
					System.err.println("JCyclone: Crashing runtime due to exception - goodbye");
					System.exit(-1);
				}
			}
		}

		signalMgr.fire(new StagesInitializedSignal());
	}

	// Start all stages
	public void startStages() {
		initStages();
		System.err.println("JCyclone: Starting stages");
		Iterator it = stagetbl.values().iterator();
		while (it.hasNext()) {
			IStageWrapper wrapper = (IStageWrapper) it.next();
			try {
				wrapper.start();
			} catch (Exception ex) {
				System.err.println("JCyclone: Caught exception starting stage "
				    + wrapper.getStage().getName() + ": " + ex);
				ex.printStackTrace();
				if (crashOnException) {
					System.err.println("JCyclone: Crashing runtime due to exception - goodbye");
					System.exit(-1);
				}
			}
		}
		// TODO: fire signal
//		signalMgr.fire(new StagesStartedSignal());
	}

	public void stopStages() {
		Iterator it = tmtbl.keySet().iterator();
		while (it.hasNext()) {
			String stagename = (String) it.next();
			IScheduler tm = (IScheduler) tmtbl.get(stagename);
			System.err.println("JCyclone: Stopping ThreadManager " + stagename);
			tm.deregisterAll();
		}
	}

	// Destroy all stages
	public void destroyStages() {
		stopStages();
		Iterator it = stagetbl.values().iterator();
		while (it.hasNext()) {
			IStageWrapper wrapper = (IStageWrapper) it.next();
			try {
				wrapper.destroy();
			} catch (Exception ex) {
				System.err.println("JCyclone: Caught exception destroying stage "
				    + wrapper.getStage().getName() + ": " + ex);
				ex.printStackTrace();
			}
		}
	}

	public void deprogramStages() throws Exception {
		destroyStages();
		Iterator it = stagetbl.values().iterator();
		while (it.hasNext()) {
			IStageWrapper wrapper = (IStageWrapper) it.next();
			try {
				wrapper.deprogram();
			} catch (Exception ex) {
				System.err.println("JCyclone: Caught exception destroying stage "
				    + wrapper.getStage().getName() + ": " + ex);
				ex.printStackTrace();
			}
		}
	}

	public void unloadStages() throws Exception {
		deprogramStages();
		Iterator it = stagetbl.keySet().iterator();
		while (it.hasNext()) {
			String stagename = (String) it.next();
			try {
				unloadStage(stagename);
			} catch (Exception ex) {
				System.err.println("JCyclone: Caught exception destroying stage " + stagename + ": " + ex);
				ex.printStackTrace();
			}
		}
	}

	/**
	 * Return the wrapper corresponding to the given stage name.
	 * This method load the wrapper if it is not already loaded.
	 */
	private IStageWrapper loadWrapper(String stagename) throws NoSuchStageException {
		if (stagename == null) throw new NoSuchStageException("no such stage: null");
		IStageWrapper wrapper = (IStageWrapper) stagetbl.get(stagename);

		if (wrapper == null) {
			System.err.println("-- Loading <" + stagename + ">");

			wrapper = new StageWrapper((ISystemManager) this, (IStageManager) this, stagename);
			stagetbl.put(stagename, wrapper);
		}
		return wrapper;
	}

	/**
	 * Return the wrapper corresponding the the given stage name.
	 */
	private IStageWrapper getWrapper(String stagename) throws NoSuchStageException {
		if (stagename == null) throw new NoSuchStageException("no such stage: null");
		IStageWrapper wrapper = (IStageWrapper) stagetbl.get(stagename);
		if (wrapper == null) throw new NoSuchStageException("no such stage: " + stagename);
		return wrapper;
	}

}

