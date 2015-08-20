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
import org.jcyclone.core.profiler.IProfilable;
import org.jcyclone.core.profiler.JCycloneProfiler;
import org.jcyclone.core.stage.IStageManager;

import java.util.*;

/**
 * ThreadPool is a generic class which provides a thread pool.
 *
 * @author Matt Welsh and Jean Morissette
 */
public class ThreadPool implements IProfilable {

	private static final boolean DEBUG = false;

	private IStageWrapper stage;
	private IStageManager mgr;
	private String poolname;
	private ThreadGroup pooltg;
	private Runnable runnable;
	private List threads;
	private List stoppingThreads;

	private int initialThreads, minThreads, maxThreads;

	private int blockTime;
	private int idleTimeThreshold;
	// wait time before interupt
	private int waitTime = 100;

	/**
	 * Lifecycle state
	 */
	volatile int runState;

	// Special values for runState
	/**
	 * Normal, not-shutdown mode
	 */
	static final int RUNNING = 0;
	/**
	 * Shutdown mode
	 */
	static final int STOP = 1;
	/**
	 * Final state
	 */
	static final int TERMINATED = 2;

	private final Object removeThreadLock = new Object();
	private final Object terminationMonitor = new Object();

	/**
	 * Create a thread pool for the given stage, manager and runnable,
	 * with the thread pool controller determining the number of threads
	 * used.
	 */
	public ThreadPool(IStageWrapper stage, IStageManager mgr, Runnable runnable) {
		ISystemConfig config = mgr.getConfig();

		// First look for stages.[stageName] options, then global options
		String tag = "stages." + (stage.getStage().getName()) + ".threadPool.";
		String globaltag = "global.threadPool.";

		int initialThreads = config.getInt(tag + "initialThreads");
		if (initialThreads < 1) {
			initialThreads = config.getInt(globaltag + "initialThreads");
			if (initialThreads < 1) initialThreads = 1;
		}
		int minThreads = config.getInt(tag + "minThreads");
		if (minThreads < 1) {
			minThreads = config.getInt(globaltag + "minThreads");
			if (minThreads < 1) minThreads = 1;
		}
		int maxThreads = config.getInt(tag + "maxThreads", 0);
		if (maxThreads == 0) {
			maxThreads = config.getInt(globaltag + "maxThreads", 0);
			if (maxThreads == 0) maxThreads = -1; // Infinite
		}

		int blockTime = config.getInt(tag + "blockTime",
		    config.getInt(globaltag + "blockTime", 1000));
		int idleTimeThreshold = config.getInt(tag + "sizeController.idleTimeThreshold",
		    config.getInt(globaltag + "sizeController.idleTimeThreshold", blockTime));

		init(stage, mgr, runnable, initialThreads, minThreads, maxThreads, blockTime, idleTimeThreshold);
	}

	/**
	 * Create a thread pool with the given name, manager, runnable,
	 * and thread sizing parameters.
	 */
	public ThreadPool(IStageWrapper stage, IStageManager mgr, Runnable runnable,
	                  int initialThreads, int minThreads, int maxThreads,
	                  int blockTime, int idleTimeThreshold) {
		init(stage, mgr, runnable, initialThreads, minThreads, maxThreads, blockTime, idleTimeThreshold);
	}

	/**
	 * Create a thread pool with the given name, manager, runnable,
	 * and a fixed number of threads.
	 */
	public ThreadPool(IStageWrapper stage, IStageManager mgr, Runnable runnable, int numThreads) {
		init(stage, mgr, runnable, numThreads, numThreads, numThreads, blockTime, idleTimeThreshold);
	}

	private void init(IStageWrapper stage, IStageManager mgr, Runnable runnable,
	                  int initialThreads, int minThreads, int maxThreads,
	                  int blockTime, int idleTimeThreshold) {
		this.stage = stage;
		this.poolname = stage.getStage().getName();
		this.mgr = mgr;
		this.runnable = runnable;
		threads = Collections.synchronizedList(new LinkedList());
		stoppingThreads = Collections.synchronizedList(new ArrayList());

		this.minThreads = minThreads;
		if (this.minThreads < 1) this.minThreads = 1;
		this.initialThreads = initialThreads;
		if (this.initialThreads < 1) this.initialThreads = this.minThreads;
		this.maxThreads = maxThreads;
		if (maxThreads > 0 && maxThreads < minThreads) this.maxThreads = -1;  // Infinite
		this.blockTime = blockTime;
		this.idleTimeThreshold = idleTimeThreshold;

		runState = TERMINATED;
		System.err.println("TP <" + poolname + ">: initial " + initialThreads + ", min " + minThreads + ", max " + maxThreads + ", blockTime " + blockTime + ", idleTime " + idleTimeThreshold);

	}

	/**
	 * Start the thread pool. Invocation has no additional effect if already started.
	 */
	public synchronized void start() {
		int state = runState;
		if (state == RUNNING) return;

		pooltg = new ThreadGroup(getName());
		addThreads(initialThreads, false);
		mgr.getProfiler().add(getName(), this);

		System.err.println(getName() + ": Starting " + numThreads() + " threads");
		for (int i = 0; i < threads.size(); i++) {
			Thread t = (Thread) threads.get(i);
			t.start();
		}

		runState = RUNNING;

		// is thread pool restarted during shut down?
		if (state == STOP) {
			synchronized (terminationMonitor) {
				// notify threads waiting to termination
				terminationMonitor.notifyAll();
			}
		}
	}

	/**
	 * Initiates a stop.
	 * Invocation has no additional effect if already stopped.
	 */
	public synchronized void stop() {
		if (runState != RUNNING) return;
		System.err.println(getName() + ": Stopping " + threads.size() + " threads");
		runState = STOP;

		// stop all active threads
		synchronized (removeThreadLock) {
			for (Iterator it = threads.iterator(); it.hasNext();) {
				Thread t = (Thread) it.next();
				it.remove();
				stoppingThreads.add(t);
			}
		}
	}

	/**
	 * Attempts to stop all threads by cancelling threads via {@link
	 * Thread#interrupt}, so if runnable implementation fails to respond to
	 * interrupts, threads may never terminate.
	 */
	public synchronized void stopNow() {
		if (runState == TERMINATED) return;
		runState = STOP;

		synchronized (removeThreadLock) {
			// stop all active threads
			for (Iterator it = threads.iterator(); it.hasNext();) {
				Thread t = (Thread) it.next();
				it.remove();
				stoppingThreads.add(t);
			}

			// cause thread to die
			System.err.println(getName() + ": Stopping now " + stoppingThreads.size() + " threads");
			for (Iterator it = stoppingThreads.iterator(); it.hasNext();) {
				Thread t = (Thread) it.next();
				t.interrupt();
			}
		}
	}

	/**
	 * Blocks until all threads have completed execution,
	 * or the timeout occurs, or the current thread is
	 * interrupted, or the thread pool is restarted,
	 * whichever happens first.
	 */
	public synchronized boolean awaitTermination(long waitTime)
	    throws InterruptedException {
		long start = System.currentTimeMillis();
		for (; ;) {
			if (runState == TERMINATED)
				return true;
			if (waitTime <= 0)
				return false;
			synchronized (terminationMonitor) {
				terminationMonitor.wait(waitTime);
			}
			// is pool restarted?
			if (runState == RUNNING)
				return false;
			waitTime = this.waitTime - (System.currentTimeMillis() - start);
		}
	}

	/**
	 * Returns true if this thread pool is running.
	 */
	public boolean isRunning() {
		return runState == RUNNING;
	}

	/**
	 * Returns true if this thread pool has been stopped.
	 */
	public boolean isStopped() {
		return runState != RUNNING;
	}

	/**
	 * Returns true if all threads have completed following stop.
	 * Note that isTerminated is never true unless
	 * either stop or stopNow was called first.
	 */
	public boolean isTerminated() {
		return runState == TERMINATED;
	}

	/**
	 * Add threads to this pool.
	 */
	public synchronized void addThreads(int num) {
		if (runState != RUNNING) return;
		addThreads(num, true);
	}

	// Call only under synch on this
	private void addThreads(int num, boolean start) {
		int numToAdd;
		int threadCount = threads.size();
		if (maxThreads < 0) {
			numToAdd = num;
		} else {
			int numTotal = Math.min(maxThreads, threadCount + num);
			numToAdd = numTotal - threadCount;
		}
		if (numToAdd <= 0) return;

		System.err.println(getName() + ": Adding " + numToAdd + " threads to pool, size " + (threadCount + numToAdd));
		for (int i = 0; i < numToAdd; i++) {
			String name = "TP-" + numThreads() + " <" + poolname + ">";
			RunnableProxy r = new RunnableProxy(runnable);
			Thread t = new Thread(pooltg, r, name);
			threads.add(t);

			// XXX JM: From a design point of view, Is-it better to
			// downcast or to create a new IStageGraph interface?
			((JCycloneProfiler) mgr.getProfiler()).getGraphProfiler().addThread(t, stage);
			if (start) t.start();
		}
	}

	/**
	 * Remove threads from pool.
	 */
	public synchronized void removeThreads(int num) {
		if (runState != RUNNING) return;
		System.err.println(getName() + ": Removing " + num + " threads from pool ");
		synchronized (removeThreadLock) {
			for (int i = 0; (i < num) && (threads.size() > minThreads); i++) {
				// cause the given thread to stop execution
				Thread t = (Thread) threads.remove(0);
				stoppingThreads.add(t);
			}
		}
	}

	/**
	 * Return the number of threads in this pool.
	 */
	public int numThreads() {
		return threads.size();
	}

	/**
	 * Used by a thread to determine its queue block time.
	 * TODO: move this method in another class
	 */
	public long getBlockTime() {
		return blockTime;
	}

	/**
	 * Used by a thread to determine whether it should exit.
	 */
	public boolean timeToStop(long idleTime) {
		Thread t = Thread.currentThread();
		if (idleTime > idleTimeThreshold) {
			synchronized (removeThreadLock) {
				if (threads.size() > minThreads) {
					// cause the given thread to stop execution
					if (threads.remove(t))
						stoppingThreads.add(t);
					return true;
				}
			}
		}
		return stoppingThreads.contains(t);
	}

	public String toString() {
		return "TP (size=" + numThreads() + ") for <" + poolname + ">";
	}

	public String getName() {
		return "ThreadPool <" + poolname + ">";
	}

	public int profileSize() {
		return threads.size();
	}

	private class RunnableProxy implements Runnable {

		Runnable proxied;

		public RunnableProxy(Runnable proxied) {
			this.proxied = proxied;
		}

		public void run() {
			proxied.run();
			// XXX JM: rerun if thread count < minThread?

//			assert runState != TERMINATED;

			Thread t = Thread.currentThread();
			synchronized (removeThreadLock) {
				if (!stoppingThreads.remove(t)) {
					// this should not happen... but just in case, we keep a consistent state
					System.err.println("Warning: thread " + t.getName() + " terminated without being requested to stop first");
					threads.remove(t);
				}
			}

			synchronized (ThreadPool.this) {
				if (runState != STOP)
					return;
				if (stoppingThreads.size() == 0) {
//					assert threads.size() == 0;
					runState = TERMINATED;
					mgr.getProfiler().remove(getName());
					synchronized (terminationMonitor) {
						terminationMonitor.notifyAll();
					}
				}
			}
		}
	}

}

