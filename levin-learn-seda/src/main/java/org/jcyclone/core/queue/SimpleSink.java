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

package org.jcyclone.core.queue;

import org.jcyclone.core.profiler.IProfilable;
import org.jcyclone.core.rtc.IEnqueuePredicate;

import java.util.List;

/**
 * The SimpleSink class is an abstract class which implements
 * 'null' functionality for most of the administrative methods
 * of ISink. This class can be extended to implement simple ISink's
 * which don't require most of the special behavior of the fully general
 * case.
 *
 * @author Matt Welsh
 * @see org.jcyclone.core.queue.ISink
 */
public abstract class SimpleSink implements ISink, IProfilable {

	/**
	 * Must be implemented by subclasses.
	 */
	public abstract void enqueue(IElement enqueueMe) throws SinkException;

	/**
	 * Calls enqueue() and returns false if SinkException occurs.
	 */
	public synchronized boolean enqueueLossy(IElement enqueueMe) {
		try {
			enqueue(enqueueMe);
			return true;
		} catch (SinkException se) {
			return false;
		}
	}

	/**
	 * Simply calls enqueue() on each item in the array. Note that this
	 * behavior <b>breaks</b> the property that <tt>enqueue_many()</tt>
	 * should be an "all or nothing" operation, since enqueue() might
	 * reject some items but not others. Don't use SimpleSink if this is
	 * going to be a problem.
	 */
	public synchronized void enqueue_many(IElement[] enqueueMe) throws SinkException {
		for (int i = 0; i < enqueueMe.length; i++) {
			enqueue(enqueueMe[i]);
		}
	}

	/**
	 * Simply calls enqueue() on each item in the list. Note that this
	 * behavior <b>breaks</b> the property that <tt>enqueueMany()</tt>
	 * should be an "all or nothing" operation, since enqueue() might
	 * reject some items but not others. Don't use SimpleSink if this is
	 * going to be a problem.
	 */
	public synchronized void enqueueMany(List enqueueMe) throws SinkException {
		for (int i = 0; i < enqueueMe.size(); i++) {
			enqueue((IElement) enqueueMe.get(i));
		}
	}

	/**
	 * Not supported; throws an IllegalArgumentException.
	 */
	public Object enqueue_prepare(IElement enqueueMe[]) throws SinkException {
		throw new IllegalArgumentException("enqueue_prepare not supported on SimpleSink objects");
	}

	public ITransaction enqueuePrepare(List elements) throws SinkException {
		throw new IllegalArgumentException("enqueuePrepare not supported on SimpleSink objects");
	}

	public void enqueuePrepare(List elements, ITransaction txn) throws SinkException {
		throw new IllegalArgumentException("enqueuePrepare not supported on SimpleSink objects");
	}

	/**
	 * Not supported; throws an IllegalArgumentException.
	 */
	public void enqueue_commit(Object key) {
		throw new IllegalArgumentException("enqueue_commit not supported on SimpleSink objects");
	}

	/**
	 * Not supported; throws an IllegalArgumentException.
	 */
	public void enqueue_abort(Object key) {
		throw new IllegalArgumentException("enqueue_abort not supported on SimpleSink objects");
	}

	/**
	 * Not supported; throws an IllegalArgumentException.
	 */
	public void setEnqueuePredicate(IEnqueuePredicate pred) {
		throw new IllegalArgumentException("setEnqueuePredicate not supported on SimpleSink objects");
	}

	/**
	 * Returns null.
	 */
	public IEnqueuePredicate getEnqueuePredicate() {
		return null;
	}

	/**
	 * Returns 0.
	 */
	public int size() {
		return 0;
	}

	public void setCapacity(int newCapacity) {
		throw new UnsupportedOperationException();
	}

	public int capacity() {
		return Integer.MAX_VALUE;
	}

	/**
	 * Returns size.
	 */
	public int profileSize() {
		return size();
	}

}
