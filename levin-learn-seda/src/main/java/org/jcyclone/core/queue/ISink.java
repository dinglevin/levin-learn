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
 * A ISink implements the 'sink' end of a finite-length event queue:
 * it supports enqueue operations only. These operations can throw a
 * SinkException if the sink is closed or becomes full, allowing event
 * queues to support thresholding and backpressure.
 *
 * @author Matt Welsh
 */
public interface ISink {

	/**
	 * Enqueues the given element onto the queue.
	 *
	 * @param element The <code>IElement</code> to enqueue
	 * @throws SinkFullException   Indicates that the sink is temporarily full.
	 * @throws SinkClosedException Indicates that the sink is
	 *                             no longer being serviced.
	 */
	void enqueue(IElement element) throws SinkException;

	/**
	 * Enqueues the given element onto the queue.
	 * <p/>
	 * This is lossy in that this method drops the element if the element
	 * could not be enqueued, rather than throwing a SinkFullException or
	 * SinkClosedException. This is meant as a convenience interface for
	 * "low priority" enqueue events which can be safely dropped.
	 *
	 * @param element The <code>IElement</code> to enqueue
	 * @return true if the element was enqueued, false otherwise.
	 */
	boolean enqueueLossy(IElement element);

	/**
	 * Given an array of elements, atomically enqueues all of the elements
	 * in the array. This guarantees that no other thread can interleave its
	 * own elements with those being inserted from this array. The
	 * implementation must enqueue all of the elements or none of them;
	 * if a SinkFullException or SinkClosedException is thrown, none of
	 * the elements will have been enqueued. This implies that the enqueue
	 * predicate (if any) must accept all elements in the array for the
	 * enqueue to proceed.
	 *
	 * @param elements The element array to enqueue
	 * @throws org.jcyclone.core.queue.SinkFullException
	 *          Indicates that the sink is temporarily full.
	 * @throws org.jcyclone.core.queue.SinkClosedException
	 *          Indicates that the sink is
	 *          no longer being serviced.
	 */
	void enqueueMany(List elements) throws SinkException;

	/**
	 * Support for transactional enqueue.
	 * <p/>
	 * <p>This method allows a client to provisionally enqueue a number
	 * of elements onto the queue, and then later commit the enqueue (with
	 * a <tt>commit()</tt> call), or abort (with a
	 * <tt>abort()</tt> call). This mechanism can be used to
	 * perform "split-phase" enqueues, where a client first enqueues a
	 * set of elements on the queue and then performs some work to "fill in"
	 * those elements before performing a commit. This can also be used
	 * to perform multi-queue transactional enqueue operations, with an
	 * "all-or-nothing" strategy for enqueueing events on multiple queues.
	 * <p/>
	 * <p>This method would generally be used in the following manner:
	 * <pre>
	 *   ITransaction txn = sink.enqueuePrepare(someElements);
	 *   if (can_commit) {
	 *     txn.commit();
	 *   } else {
	 *     txn.abort();
	 *   }
	 * </pre>
	 * <p/>
	 * <p>Like <tt>enqueueMany</tt>, <tt>enqueuePrepare</tt> is an
	 * "all or none" operation: the enqueue predicate must accept all
	 * elements for enqueue, or none of them will be enqueued.
	 *
	 * @param elements The element array to provisionally enqueue
	 * @return A transaction object that may be used to commit or abort
	 *         the provisional enqueue
	 * @throws org.jcyclone.core.queue.SinkFullException
	 *          Indicates that the sink is temporarily full
	 *          and that the requested elements could not be provisionally enqueued.
	 * @throws org.jcyclone.core.queue.SinkClosedException
	 *          Indicates that the sink is
	 *          no longer being serviced.
	 */
	ITransaction enqueuePrepare(List elements) throws SinkException;

	void enqueuePrepare(List elements, ITransaction txn) throws SinkException;

	/**
	 * Return the number of elements in this sink.
	 */
	int size();

	/**
	 * Reset the capacity of this queue.
	 * If the new capacity is less than the old capacity, existing
	 * elements are not removed, but incoming puts will not proceed
	 * until the number of elements is less than the new capacity.
	 *
	 * @throws UnsupportedOperationException if the <tt>setCapacity</tt>
	 *                                       method is not supported by this sink.
	 * @throws IllegalArgumentException      if capacity less or equal to zero
	 */
	void setCapacity(int newCapacity);

	/**
	 * Return the number of elements this sink can hold.
	 */
	int capacity();
}
