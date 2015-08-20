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

package org.jcyclone.core.rtc;

import org.jcyclone.core.queue.IElement;
import org.jcyclone.core.queue.ISink;

import java.util.List;

/**
 * This enqueue predicate implements a simple threshold for the
 * size of the queue.
 */
public class QueueThresholdPredicate implements IEnqueuePredicate {

	private static final boolean DEBUG = false;

	private ISink thesink;

	/**
	 * Create a new QueueThresholdPredicate for the given sink and
	 * threshold. A threshold of -1 indicates no threshold.
	 */
	public QueueThresholdPredicate(ISink sink, int threshold) {
		this.thesink = sink;
		setThreshold(threshold);
	}

	/**
	 * Returns true if the given element can be accepted into the queue.
	 */
	public boolean accept(IElement qel) {
		if (DEBUG)
			System.err.println("QueueThresholdPredicate.accept [" + thesink + "]: size " + thesink.size()
			    + ", thresh " + thesink.capacity());
		return true;
	}

	public void blockingAccept(IElement element) {
		// do nothing
	}

	public boolean acceptMany(List elements) {
		// do nothing
		return true;
	}

	/**
	 * Return the current queue threshold.
	 */
	public int getThreshold() {
		return thesink.capacity();
	}

	/**
	 * Set the current queue threshold. A queue threshold of -1 indicates
	 * an infinite threshold.
	 */
	public void setThreshold(int threshold) {
		if (threshold <= -1)
			thesink.setCapacity(Integer.MAX_VALUE);
		else
			thesink.setCapacity(threshold);
	}

}
