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

import org.jcyclone.core.cfg.ISystemConfig;
import org.jcyclone.core.handler.IEventHandler;
import org.jcyclone.core.handler.ISingleThreadedEventHandler;
import org.jcyclone.core.queue.ISource;
import org.jcyclone.core.rtc.IResponseTimeController;
import org.jcyclone.core.stage.IStageManager;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

/**
 * TPSScheduler provides a threadpool-per-stage scheduler implementation.
 *
 * @author Matt Welsh and Jean Morissette
 */
public class TPSScheduler implements IScheduler {

	private static final boolean DEBUG = false;
	private static final boolean DEBUG_VERBOSE = false;

	protected IStageManager mgr;
	protected ISystemConfig config;
	protected Hashtable srTbl;     // IStageWrapper --> stageRunnable
	protected ThreadPoolController sizeController;
	protected boolean crashOnException;

	public TPSScheduler(IStageManager mgr) {
		this(mgr, true);
	}

	public TPSScheduler(IStageManager mgr, boolean initialize) {
		this.mgr = mgr;
		this.config = mgr.getConfig();

		if (initialize) {
			if (config.getBoolean("global.threadPool.sizeController.enable")) {
				sizeController = new ThreadPoolController(mgr);
			}
			srTbl = new Hashtable();
		}

		crashOnException = config.getBoolean("global.crashOnException");
	}

	/**
	 * Register a stage with this thread manager.
	 */
	public synchronized void register(IStageWrapper stage) {
		if (srTbl.contains(stage))
			throw new IllegalStateException("Stage " + stage.getStage().getName() + " already registered");
		// Create a threadPool for the stage
		stageRunnable sr = new stageRunnable(stage);
		srTbl.put(stage, sr);
	}

	/**
	 * Deregister a stage with this thread manager.
	 */
	public synchronized void deregister(IStageWrapper stage) {
		stageRunnable sr = (stageRunnable) srTbl.get(stage);
		if (sr == null) {
			throw new IllegalStateException("Stage " + stage.getStage().getName() + " not registered");
		}
		sr.stop();
		srTbl.remove(sr);
	}

	/**
	 * Stop the thread manager and all threads managed by it.
	 */
	public synchronized void deregisterAll() {
		Enumeration e = srTbl.elements();
		while (e.hasMoreElements()) {
			stageRunnable sr = (stageRunnable) e.nextElement();
			sr.stop();
			srTbl.remove(sr);
		}
	}

	/**
	 * Wake any thread waiting for work.  This is called by
	 * an enqueue* method of FiniteQueue.
	 */
	public void wake() { /* do nothing*/
	}

	/**
	 * Internal class representing the Runnable for a single stage.
	 */
	protected class stageRunnable implements Runnable {

		protected ThreadPool tp;
		protected IStageWrapper wrapper;
		protected IBatchSorter sorter;
		protected IEventHandler handler;
		protected ISource source;
		protected String name;
		protected IResponseTimeController rtController = null;
		protected boolean firstToken = false;
		protected int blockTime = -1;
		protected int terminationTimeout = 100;


		protected stageRunnable(IStageWrapper wrapper) {
			this.wrapper = wrapper;
			// Create a threadPool for the stage
			if (wrapper.getEventHandler() instanceof ISingleThreadedEventHandler) {
				tp = new ThreadPool(wrapper, mgr, this, 1);
			} else {
				tp = new ThreadPool(wrapper, mgr, this);
			}
			this.init();
		}

		private void init() {
			this.source = wrapper.getSource();
			this.handler = wrapper.getEventHandler();
			this.name = wrapper.getStage().getName();
			this.rtController = wrapper.getResponseTimeController();

			blockTime = (int) tp.getBlockTime();
			if (sizeController != null) {
				// The sizeController is globally enabled -- has the user disabled
				// it for this stage?
				String val = config.getString("stages." + this.name + ".threadPool.sizeController.enable");
				if ((val == null) || val.equals("true") || val.equals("TRUE")) {
					sizeController.register(wrapper, tp);
				}
			}

			this.sorter = wrapper.getBatchSorter();
			if (this.sorter == null) {
				// XXX MDW: Should be ControlledBatchSorter
				this.sorter = new NullBatchSorter();
			}
			sorter.init(wrapper, mgr);

			tp.start();
		}

		void stop() {
			Thread.interrupted(); // reset the status
			tp.stop();
			boolean terminated = false;
			while (true) {
				try {
					terminated = tp.awaitTermination(terminationTimeout);
					if (terminated)
						break;
					else
						tp.stopNow();
				} catch (InterruptedException ie) {
					// reset the status and roll-forward
					Thread.interrupted();
					tp.stopNow();
				}
			}
		}

		public void run() {
			long t1, t2;
			long tstart = 0, tend = 0;

			if (DEBUG) System.err.println(name + ": starting, source is " + source);

			t1 = System.currentTimeMillis();

			while (true) {

				try {
					if (DEBUG_VERBOSE) System.err.println(name + ": Doing blocking dequeue for " + wrapper);

					Thread.yield(); // only accomplishes delay

					// Run any pending batches
					boolean ranbatch = false;
					IBatchDescr batch;

					while ((batch = sorter.nextBatch(blockTime)) != null) {
						ranbatch = true;
						List events = batch.getBatch();
						if (DEBUG_VERBOSE) System.err.println("<" + name + ">: Got batch of " + events.size() + " events");

						// Call event handler
						tstart = System.currentTimeMillis();
						handler.handleEvents(events);
						batch.batchDone();
						tend = System.currentTimeMillis();

						// Record service rate
						wrapper.getStats().recordServiceRate(events.size(), tend - tstart);

						// Run response time controller
						if (rtController != null) {
							rtController.adjustThreshold(events, tend - tstart);
						}
					}

					// Check if idle
					if (!ranbatch) {
						t2 = System.currentTimeMillis();
						if (tp.timeToStop(t2 - t1)) {
							if (DEBUG) System.err.println(name + ": Exiting");
							return;
						}
						continue;
					}

					t1 = System.currentTimeMillis();

					if (tp.timeToStop(0)) {
						if (DEBUG) System.err.println(name + ": Exiting");
						return;
					}

				} catch (InterruptedException e) {
					return;
				} catch (Exception e) {
					System.err.println("JCyclone: Stage <" + name + "> got exception: " + e);
					e.printStackTrace();
					if (crashOnException) {
						System.err.println("JCyclone: Crashing runtime due to exception - goodbye");
						System.exit(-1);
					}
				}
			}
		}
	}

}

