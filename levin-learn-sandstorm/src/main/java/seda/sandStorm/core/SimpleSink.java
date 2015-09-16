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

package seda.sandstorm.core;

import seda.sandstorm.api.EnqueuePredicate;
import seda.sandstorm.api.EventElement;
import seda.sandstorm.api.EventSink;
import seda.sandstorm.api.Profilable;
import seda.sandstorm.api.SinkException;

/**
 * The SimpleSink class is an abstract class which implements 'null'
 * functionality for most of the administrative methods of SinkIF. This class
 * can be extended to implement simple SinkIF's which don't require most of the
 * special behavior of the fully general case.
 *
 * @author Matt Welsh
 * @see seda.sandstorm.api.EventSink
 */
public abstract class SimpleSink implements EventSink, Profilable {

    /**
     * Must be implemented by subclasses.
     */
    public abstract void enqueue(EventElement event) throws SinkException;

    /**
     * Calls enqueue() and returns false if SinkException occurs.
     */
    public synchronized boolean enqueueLossy(EventElement event) {
        try {
            enqueue(event);
            return true;
        } catch (SinkException se) {
            return false;
        }
    }

    /**
     * Simply calls enqueue() on each item in the array. Note that this behavior
     * <b>breaks</b> the property that <tt>enqueue_many()</tt> should be an
     * "all or nothing" operation, since enqueue() might reject some items but
     * not others. Don't use SimpleSink if this is going to be a problem.
     */
    public synchronized void enqueueMany(EventElement[] events)
            throws SinkException {
        for (int i = 0; i < events.length; i++) {
            enqueue(events[i]);
        }
    }

    /**
     * Not supported; throws an IllegalArgumentException.
     */
    public Object enqueuePrepare(EventElement events[]) throws SinkException {
        throw new IllegalArgumentException(
                "enqueue_prepare not supported on SimpleSink objects");
    }

    /**
     * Not supported; throws an IllegalArgumentException.
     */
    public void enqueueCommit(Object key) {
        throw new IllegalArgumentException(
                "enqueue_commit not supported on SimpleSink objects");
    }

    /**
     * Not supported; throws an IllegalArgumentException.
     */
    public void enqueueAbort(Object key) {
        throw new IllegalArgumentException(
                "enqueue_abort not supported on SimpleSink objects");
    }

    /**
     * Not supported; throws an IllegalArgumentException.
     */
    public void setEnqueuePredicate(EnqueuePredicate pred) {
        throw new IllegalArgumentException(
                "setEnqueuePredicate not supported on SimpleSink objects");
    }

    /**
     * Returns null.
     */
    public EnqueuePredicate getEnqueuePredicate() {
        return null;
    }

    /**
     * Returns 0.
     */
    public int size() {
        return 0;
    }

    /**
     * Returns size.
     */
    public int profileSize() {
        return size();
    }
}
