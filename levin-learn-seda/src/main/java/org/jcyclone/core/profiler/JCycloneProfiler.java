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


package org.jcyclone.core.profiler;

import org.jcyclone.core.cfg.ISystemConfig;
import org.jcyclone.core.internal.StageGraph;
import org.jcyclone.core.stage.IStageManager;

import java.util.ArrayList;
import java.util.List;

/**
 * JCycloneProfiler is an implementation of the IProfiler interface
 * for JCyclone. It is implemented using a thread that periodically
 * samples the set of ProfilableIF's registered with it, and outputs
 * the profile to registered IProfilerHandler.
 *
 * @author Matt Welsh and Jean Morissette
 * @see IProfiler
 * @see IProfilable
 * @see IProfilerHandler
 * @see IProfilerFilter
 */
public class JCycloneProfiler implements IProfiler {

	private int delay;
	private List profiles;
	private IProfilerFilter filter;
	private List filteredProfiles;
	private List handlers;
	private IStageManager mgr;
	private StageGraph graphProfiler;
	private ProfilerRunner runner;
	int[] snapshot;

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

	public JCycloneProfiler(IStageManager mgr) {
		this.mgr = mgr;
		handlers = new ArrayList();
		profiles = new ArrayList();
		filteredProfiles = new ArrayList();
		graphProfiler = new StageGraph(mgr);
		runState = STOP;
		ISystemConfig config = mgr.getConfig();

		String filterClassname = config.getString("global.profile.filter.class");
		if (filterClassname != null) {
			try {
				filter = (IProfilerFilter) Class.forName(filterClassname).newInstance();
				filter.init(config);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		String handlerClassname = config.getString("global.profile.handler.class");
		if (handlerClassname != null) {
			try {
				IProfilerHandler handler = (IProfilerHandler) Class.forName(handlerClassname).newInstance();
				addHandler(handler);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		add("usedmem(kb)", new UsedMemory());
		add("freemem(kb)", new FreeMemory());
		add("totalmem(kb)", new TotalMemory());

		boolean enable = config.getBoolean("global.profile.enable");
		if (enable) start();
	}

	public synchronized void start() {
		if (runState == RUNNING) return;
		runState = RUNNING;

		int delay = mgr.getConfig().getInt("global.profile.delay");
		if (delay != this.delay) {
			this.delay = delay;
			fireDelayChanged(delay);
		}

		int prio = mgr.getConfig().getInt("global.profile.prio", Thread.MAX_PRIORITY);
		runner = new ProfilerRunner();
		runner.setPriority(prio);
		runner.start();
	}

	public synchronized void stop() {
		if (runState == STOP) return;
		runState = STOP;
		runner.stop = true;
		runner.interrupt();
	}

	synchronized void destroy() {
		stop();
		for (int i = 0; i < handlers.size(); i++) {
			IProfilerHandler h = (IProfilerHandler) handlers.get(i);
			h.destroy();
		}
		handlers.clear();
	}

	/**
	 * Returns true if this thread pool is running.
	 */
	public boolean isRunning() {
		return runState == RUNNING;
	}

	/**
	 * Add a class to this profiler.
	 */
	public synchronized void add(String name, IProfilable pr) {
		if (name == null || pr == null)
			return;
		profile p = new profile(name, pr);
		if (profiles.contains(p)) {
			System.err.println("Profiler: Duplicate profilable name '" + name + "'");
			return;
		}
		profiles.add(p);
		if (filter == null || filter.isProfilable(p.name)) {
			filteredProfiles.add(p);
			snapshot = new int[filteredProfiles.size()];
			fireProfilableAdded(p.name);
		}
	}

	public synchronized void remove(String name) {
		if (name == null)
			return;
		profile dummy = new profile(name, null);
		if (profiles.remove(dummy)) {
			if (filteredProfiles.remove(dummy)) {
				snapshot = new int[filteredProfiles.size()];
				fireProfilableRemoved(name);
			}
		}
	}

	public synchronized void addHandler(IProfilerHandler handler) {
		if (handler == null)
			return;
		if (handlers.contains(handler)) {
			System.err.println("Profiler: Duplicate handler '" + handler + "'");
			return;
		}
		handler.init(mgr);
		handlers.add(handler);
		for (int i = 0; i < filteredProfiles.size(); i++) {
			profile profile = (profile) filteredProfiles.get(i);
			handler.profilableAdded(profile.name);
		}
	}

	public synchronized void removeHandler(IProfilerHandler handler) {
		if (handler == null)
			return;
		if (handlers.remove(handler))
			handler.destroy();
	}

	class ProfilerRunner extends Thread {

		boolean stop;

		public ProfilerRunner() {
			super("Profiler");
		}

		public void run() {
			while (true) {
				synchronized (this) {

					if (stop) return;

					// To have an accurate snapshot of the system state, we call profileSize()
					// for all IProfilable objects in a very short amount of time.
					int size = filteredProfiles.size();
					for (int i = 0; i < size; i++) {
						profile p = (profile) filteredProfiles.get(i);
						snapshot[i] = p.pr.profileSize();
					}
					fireProfilablesSnapshot(snapshot);
				}
				try {
					Thread.sleep(delay);
				} catch (InterruptedException ie) {
					return;
				}
			}
		}
	}

	public StageGraph getGraphProfiler() {
		return graphProfiler;
	}

	private void fireProfilableAdded(String name) {
		for (int i = 0; i < handlers.size(); i++) {
			IProfilerHandler handler = (IProfilerHandler) handlers.get(i);
			handler.profilableAdded(name);
		}
	}

	private void fireProfilableRemoved(String name) {
		for (int i = 0; i < handlers.size(); i++) {
			IProfilerHandler handler = (IProfilerHandler) handlers.get(i);
			handler.profilableRemoved(name);
		}
	}

	private void fireProfilablesSnapshot(int[] snapshot) {
		for (int i = 0; i < handlers.size(); i++) {
			IProfilerHandler handler = (IProfilerHandler) handlers.get(i);
			handler.profilablesSnapshot(snapshot);
		}
	}

	private void fireDelayChanged(int delay) {
		for (int i = 0; i < handlers.size(); i++) {
			IProfilerHandler h = (IProfilerHandler) handlers.get(i);
			h.sampleDelayChanged(delay);
		}
	}

	class profile {
		String name;
		IProfilable pr;

		profile(String name, IProfilable pr) {
			this.name = name;
			this.pr = pr;
		}

		public boolean equals(Object o) {
			if (this == o) return true;
			if (!(o instanceof profile)) return false;

			final profile profile = (profile) o;

			return name.equals(profile.name);
		}

		public int hashCode() {
			return name.hashCode();
		}
	}

	class TotalMemory implements IProfilable {
		public int profileSize() {
			return (int) (Runtime.getRuntime().totalMemory() / 1024);
		}
	}

	class FreeMemory implements IProfilable {
		public int profileSize() {
			return (int) (Runtime.getRuntime().freeMemory() / 1024);
		}
	}

	class UsedMemory implements IProfilable {
		public int profileSize() {
			long totalmem = Runtime.getRuntime().totalMemory() / 1024;
			long freemem = Runtime.getRuntime().freeMemory() / 1024;
			return (int) (totalmem - freemem);
		}
	}

}
