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

import org.jcyclone.core.profiler.IProfilable;
import org.jcyclone.core.profiler.JCycloneProfiler;
import org.jcyclone.core.queue.IElement;
import org.jcyclone.core.queue.ISink;
import org.jcyclone.core.queue.ITransaction;
import org.jcyclone.core.queue.SinkException;
import org.jcyclone.core.stage.IStageManager;

import java.util.Hashtable;
import java.util.List;

/**
 * Used as a proxy to observe and measure communication behavior between
 * stages. By handing out a sink proxy, it is
 * possible to gather statistics on event communication between stages.
 * This is used by StageGraph to construct a graph of the communication
 * patterns between stages.
 *
 * @author Matt Welsh
 */
public class MonitoredSink implements ISink, IProfilable {

	private static final boolean DEBUG = false;

	private IStageWrapper toStage;
	private StageGraph stageGraph;
	public ISink thesink;
	private Thread client = null;
	private Hashtable clientTbl = null;

	/**
	 * Maintains a running sum of the number of elements enqueued onto
	 * this sink.
	 */
	public int enqueueCount;

	/**
	 * Maintains a running sum of the number of elements successfully
	 * enqueued onto this sink (that is, not rejected by the enqueue predicate).
	 */
	public int enqueueSuccessCount;

	/**
	 * Used to maintain a timer for statistics gathering.
	 */
	public long timer;

	/**
	 * Create a SinkProxy for the given sink.
	 *
	 * @param sink    The sink to create a proxy for.
	 * @param mgr     The associated manager.
	 * @param toStage The stage which this sink pushes events to.
	 */
	public MonitoredSink(ISink sink, IStageManager mgr, IStageWrapper toStage) {
		this.thesink = sink;
		this.stageGraph = ((JCycloneProfiler) mgr.getProfiler()).getGraphProfiler();
		this.toStage = toStage;
		this.enqueueCount = 0;
		this.enqueueSuccessCount = 0;
		this.timer = 0;
	}

	/**
	 * Return the size of the queue.
	 */
	public int size() {
		return thesink.size();
	}

	public void setCapacity(int newCapacity) {
		thesink.setCapacity(newCapacity);
	}

	public int capacity() {
		return thesink.capacity();
	}

	public void enqueue(IElement enqueueMe) throws SinkException {
		recordUse();
		enqueueCount++;
		thesink.enqueue(enqueueMe);
		enqueueSuccessCount++;
	}

	public boolean enqueueLossy(IElement enqueueMe) {
		recordUse();
		enqueueCount++;
		boolean pass = thesink.enqueueLossy(enqueueMe);
		if (pass) enqueueSuccessCount++;
		return pass;
	}

	public void enqueueMany(List list) throws SinkException {
		recordUse();
		if (list != null) {
			enqueueCount += list.size();
		}
		thesink.enqueueMany(list);
		if (list != null) {
			enqueueSuccessCount += list.size();
		}
	}

	/**
	 * Return the profile size of the queue.
	 */
	public int profileSize() {
		return size();
	}

	public ITransaction enqueuePrepare(List elements) throws SinkException {
		recordUse();
		if (elements != null) {
			enqueueCount += elements.size();
		}
		ITransaction key = thesink.enqueuePrepare(elements);
		if (elements != null) {
			enqueueSuccessCount += elements.size();
		}
		return key;
	}

	public void enqueuePrepare(List elements, ITransaction txn) throws SinkException {
		recordUse();
		if (elements != null) {
			enqueueCount += elements.size();
		}
		thesink.enqueuePrepare(elements, txn);
		if (elements != null) {
			enqueueSuccessCount += elements.size();
		}
	}

	public String toString() {
		return "[SinkProxy for toStage=" + toStage + "]";
	}

	private void recordUse() {
		if (DEBUG) System.err.println("SinkProxy: Recording use of " + this + " by thread " + Thread.currentThread());

		if (client == null) {
			client = Thread.currentThread();

			StageGraphEdge edge = new StageGraphEdge();
			edge.fromStage = stageGraph.getStageFromThread(client);
			edge.toStage = toStage;
			edge.sink = this;
			stageGraph.addEdge(edge);

		} else {
			Thread t = Thread.currentThread();
			if (client != t) {
				if (clientTbl == null) clientTbl = new Hashtable();
				if (clientTbl.get(t) == null) {
					clientTbl.put(t, t);

					StageGraphEdge edge = new StageGraphEdge();
					edge.fromStage = stageGraph.getStageFromThread(t);
					edge.toStage = toStage;
					edge.sink = this;
					stageGraph.addEdge(edge);
				}
			}
		}
	}

}
