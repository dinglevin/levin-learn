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

import java.util.List;

/**
 * A ISource implements the 'source side' of an event queue: it supports
 * dequeue operations only.
 *
 * @author Matt Welsh and Jean Morissette
 */
public interface ISource {

	/**
	 * Dequeues the next element, or returns <code>null</code> if there is
	 * nothing left on the queue.
	 *
	 * @return the next <code>IElement</code> on the queue
	 */
	IElement dequeue();

	/**
	 * Dequeues all available elements.
	 *
	 * @return all pending <code>IElement</code>s on the queue
	 */
	int dequeueAll(List list);

	/**
	 * Dequeues at most <code>maxElements</code> available elements, or returns
	 * <code>null</code> if there is nothing left on the queue.
	 *
	 * @return At most <code>num</code> <code>IElement</code>s on the queue
	 */
	int dequeue(List list, int maxElements);

	/**
	 * Just like blocking_dequeue_all, but returns only a single element.
	 */
	IElement blockingDequeue(int timeout_millis) throws InterruptedException;

	/**
	 * This method blocks on the queue up until a timeout occurs or
	 * until an element appears on the queue. It returns all elements waiting
	 * on the queue at that time.
	 *
	 * @param msecs if msecs is <code>0</code>, this method
	 *              will be non-blocking and will return right away, whether or not
	 *              any elements are pending on the queue.  If timeout_millis is
	 *              <code>-1</code>, this method blocks forever until something is
	 *              available.  If timeout_millis is positive, this method will wait
	 *              about that number of milliseconds before returning, but possibly a
	 *              little more.
	 * @return an array of <code>IElement</code>'s.  This array will
	 *         be null if no elements were pending.
	 *         <p/>
	 *         XXX: blockingDequeueAll(list, 0) have the same semantic than dequeueAll!
	 */
	int blockingDequeueAll(List list, int msecs) throws InterruptedException;

	/**
	 * This method blocks on the queue up until a timeout occurs or
	 * until an element appears on the queue. It returns at most
	 * <code>maxElements</code> elements waiting on the queue at that time.
	 */
	int blockingDequeue(List list, int msecs, int maxElements) throws InterruptedException;

	/**
	 * Returns the number of elements waiting in this queue.
	 */
	int size();

}

