/* 
 * Copyright (c) 2002 by Matt Welsh and The Regents of the University of 
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

import org.jcyclone.core.queue.ISource;
import org.jcyclone.core.stage.IStageManager;

import java.util.ArrayList;
import java.util.List;

/**
 * An implementation of BatchSorter that uses the AggThrottle
 * mechanism to automatically determine the batch size.
 */
public class AggThrottleBatchSorter implements IBatchSorter {

	private static final boolean DEBUG = false;

	private String name;
	private AggThrottle aggThrottle;
	private ISource source;
	private ThreadLocalList list;

	public AggThrottleBatchSorter() {
		list = new ThreadLocalList();
	}

	/**
	 * Called by the thread manager to associate a queue with this
	 * batch sorter.
	 */
	public void init(IStageWrapper stage, IStageManager mgr) {
		this.aggThrottle = new AggThrottle(stage, mgr);
		this.source = stage.getSource();
		this.name = stage.getStage().getName();
	}

	/**
	 * Returns a single batch for processing by the stage's event handler.
	 * Blocks until a batch can be returned.
	 */
	public IBatchDescr nextBatch(int timeout) throws InterruptedException {

		final List buffer = (List) list.get();

		int aggTarget = aggThrottle.getAggTarget();

		int num;
		if (aggTarget == -1) {
			num = source.blockingDequeueAll(buffer, timeout);
		} else {
			num = source.blockingDequeue(buffer, timeout, aggTarget);
		}

		if (num == 0)
			return null;
		else
			return new IBatchDescr() {
				public List getBatch() {
					return buffer;
				}

				public void batchDone() {
					buffer.clear();
				}
			};
	}

	class ThreadLocalList extends ThreadLocal {
		public Object initialValue() {
			return new ArrayList();
		}
	}

}
