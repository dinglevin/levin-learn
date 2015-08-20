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

package org.jcyclone.core.signal;

import org.jcyclone.core.cfg.ISystemConfig;
import org.jcyclone.core.queue.IBlockingQueue;
import org.jcyclone.core.queue.LinkedBlockingQueue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The SignalMgr is an implementation of ISignalMgr. It allows signal handlers
 * to register to receive certain signals and delivers those signals once
 * they are triggered.
 * <p/>
 * Signal handlers are registered with the chain of superclasses for each signal
 * as well, so that triggering a superclass of a given signal will also
 * reach those handlers registered for the subclass.
 *
 * @author Matt Welsh and Jean Morissette
 * @see ISignalMgr
 * @see org.jcyclone.core.signal.ISignal
 */
public class JCycloneSignalMgr implements ISignalMgr {

	private ISystemConfig config;
	private Map signalToHandlerTbl; // Map signal type to List of handlers
	private Thread thread;
	private IBlockingQueue signalQueue;
	private volatile boolean blocked;  // Indicate if the thread is blocked in blocking_dequeue

	volatile int runState;
	private static final int RUNNING = 1;
	private static final int STOP = 2;

	public JCycloneSignalMgr(ISystemConfig conf) {
		signalToHandlerTbl = new HashMap();
		signalQueue = new LinkedBlockingQueue();
		this.config = conf;
	}

	public synchronized void register(ISignal signalType, ISignalHandler handler) {
		Class type = signalType.getClass();
		List listeners = (List) signalToHandlerTbl.get(type);

		// check preconditions
		if (handler == null)
			throw new NullPointerException();
		if (listeners != null && listeners.contains(handler))
			throw new IllegalArgumentException("Handler " + handler + " already registered for signal type " + type);

		do {
			if (listeners == null) {
				listeners = new ArrayList();
				signalToHandlerTbl.put(type, listeners);
			} else {
				if (listeners.contains(handler))
					break;
			}
			listeners.add(handler);
			type = type.getSuperclass();
			listeners = (List) signalToHandlerTbl.get(type);
		} while (ISignal.class.isAssignableFrom(type));
	}

	public synchronized void deregister(ISignal signalType, ISignalHandler handler) {
		Class type = signalType.getClass();
		List listeners = (List) signalToHandlerTbl.get(type);

		// check preconditions
		if (listeners == null || !listeners.contains(handler))
			throw new IllegalArgumentException("Handler " + handler + " not registered for signal type " + type);

		do {
			// XXX: use a better data structure to avoid to keep the lock
			synchronized (listeners) {
				listeners.remove(handler);
			}
			type = type.getSuperclass();
			listeners = (List) signalToHandlerTbl.get(type);
		} while (ISignal.class.isAssignableFrom(type));
	}

	public void fire(final ISignal signal) {
		try {
			signalQueue.blockingEnqueue(signal);
		} catch (InterruptedException ignore) {
			// this must not occur
		}
	}

	synchronized void start() {
		if (runState == RUNNING) return;
		runState = RUNNING;
		int prio = config.getInt("global.signal.prio", Thread.MAX_PRIORITY);
		thread = new Thread(new SignalPublisher(), "Thread-SignalMgr");
		thread.setPriority(prio);
		thread.start();
	}

	public synchronized void stop() {
		runState = STOP;
		// XXX JM: warning, we don't want to interrupt a blocking *enqueue*!
		if (blocked) thread.interrupt();
	}

	class SignalPublisher implements Runnable {

		public void run() {
			while (true) {
				ISignal signal = null;

				blocked = true;
				if (runState == STOP) return;

				try {
					signal = (ISignal) signalQueue.blockingDequeue(-1);
				} catch (InterruptedException e) {
					return;
				}

				blocked = false;
				if (runState == STOP) return;

				List listeners = (List) signalToHandlerTbl.get(signal);
				synchronized (listeners) {
					for (int i = 0; i < listeners.size(); i++) {
						ISignalHandler h = (ISignalHandler) listeners.get(i);
						try {
							h.handleSignal(signal);
						} catch (Throwable t) {
							t.printStackTrace();
						}
					}
				}
			}
		}
	}

}
