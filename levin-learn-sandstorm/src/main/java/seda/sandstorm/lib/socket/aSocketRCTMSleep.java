/* 
 * Copyright (c) 2000 by Matt Welsh and The Regents of the University of 
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

package seda.sandstorm.lib.socket;

import seda.sandstorm.api.EventElement;
import seda.sandstorm.api.Manager;
import seda.sandstorm.api.internal.ThreadManager;

/**
 * aSocketRCTMSleep is a version of aSocketThreadManager that incorporates
 * a rate controller: given a target packet-processing rate, it adjusts
 * its schedule to attempt to match that rate. The controller is based
 * on adding controlled pauses to the packet-processing loop.
 * 
 * @author   Matt Welsh
 */
class aSocketRCTMSleep extends SocketThreadManager implements ThreadManager, aSocketConst {

  private static final boolean DEBUG = false;
  private static final int INITIAL_SLEEPTIME = 1;
  private static final int INITIAL_SLEEPFREQ = 1;
  private static final int MAX_AGGREGATION = 32;
  private double targetRate;

  aSocketRCTMSleep(Manager mgr) {
    super(mgr);
    this.targetRate = mgr.getConfig().getInt("global.aSocket.rateController.rate");
    System.err.println("aSocketRCTMSleep: Created, target rate "+targetRate);
  }

  protected aSocketThread makeThread(SocketStageWrapper wrapper) {
    return new aSocketRCThread(wrapper);
  }

  /**
   * Internal class representing a single aSocketTM-managed thread.
   */
  protected class aSocketRCThread extends SocketThreadManager.aSocketThread implements Runnable {
    private final long MIN_USEFUL_SLEEP = 10;

    protected aSocketRCThread(SocketStageWrapper wrapper) {
      super(wrapper);
    }

    public void run() {
      if (DEBUG) System.err.println(name+": starting, selsource="+selsource+", eventQ="+eventQ+", targetRate="+targetRate);

      long t1, t2;
      int num_measurements = 0, num_events = 0;
      long sleeptime = INITIAL_SLEEPTIME;
      int sleepfreq = INITIAL_SLEEPFREQ;
      int aggTarget;

      t1 = System.currentTimeMillis();

      while (true) {

	try {

	  aggTarget = tp.getAggregationTarget();

	  while (selsource.numActive() == 0) {
	    if (DEBUG) System.err.println(name+": numActive is zero, waiting on event queue");
	    EventElement qelarr[];
	    if (aggTarget == -1) {
	      qelarr = eventQ.blockingDequeueAll(EVENT_QUEUE_TIMEOUT);
	    } else {
	      qelarr = eventQ.blockingDequeue(EVENT_QUEUE_TIMEOUT, aggTarget);
	    }
	    if (qelarr != null) {
	      if (DEBUG) System.err.println(name+": got "+qelarr.length+" new requests");
	      num_events += qelarr.length;
	      handler.handleEvents(qelarr);
	    }
	  }

	  for (int s = 0; s < SELECT_SPIN; s++) {
	    if (DEBUG) System.err.println(name+": doing select, numActive "+selsource.numActive());
	    SelectQueueElement ret[];
	    if (aggTarget == -1) {
	      ret = (SelectQueueElement[])selsource.blockingDequeueAll(SELECT_TIMEOUT);
	    } else {
	      ret = (SelectQueueElement[])selsource.blockingDequeue(SELECT_TIMEOUT, aggTarget);
	    }
	    if (ret != null) {
	      if (DEBUG) System.err.println(name+": select got "+ret.length+" elements");
	      num_events += ret.length;
	      handler.handleEvents(ret);
	    } else if (DEBUG) System.err.println(name+": select got null");
	  }

	  if (DEBUG) System.err.println(name+": Checking request queue");
	  for (int s = 0; s < EVENT_QUEUE_SPIN; s++) {
	    EventElement qelarr[];
	    if (aggTarget == -1) {
	      qelarr = eventQ.dequeueAll();
	    } else {
	      qelarr = eventQ.dequeue(aggTarget);
	    }
	    if (qelarr != null) {
	      if (DEBUG) System.err.println(name+": got "+qelarr.length+" new requests");
	      num_events += qelarr.length;
	      handler.handleEvents(qelarr);
	      break;
	    }
	  }

	} catch (Exception e) {
	  System.err.println(name+": got exception "+e);
	  e.printStackTrace();
	}

	if (((num_measurements % sleepfreq) == 0) && (sleeptime > 0)) {
	  try {
	    Thread.currentThread().sleep(sleeptime);
	  } catch (InterruptedException ie) {
	    // Ignore
	  }
	}

	t2 = System.currentTimeMillis();
	num_measurements++;

	if ((num_measurements % MEASUREMENT_SIZE) == 0) {
	  double timesec = ((t2-t1)*1.0e-3);
	  double actualrate = num_events / timesec;
	  System.err.println("aSocketRCTMSleep ("+name+"): time "+ timesec+", num_events "+num_events);
	  //if (DEBUG) 
	  System.err.println("aSocketRCTMSleep ("+name+"): Rate is "+ actualrate +", target "+targetRate+", sleeptime "+sleeptime);

	  if ((actualrate >= (1.05 * targetRate)) ||
	      (actualrate <= (0.95 * targetRate))) {
	    // Update delay 
	    double delay = (num_events / targetRate) - timesec;
	    sleeptime = (long)(delay * 1.0e3);
	    if (sleeptime < 0) sleeptime = 0;
	    if ((sleeptime > 0) &&
       		((sleeptime / MEASUREMENT_SIZE) < MIN_USEFUL_SLEEP)) {
	      sleeptime = MIN_USEFUL_SLEEP;
	      sleepfreq = (int)(MIN_USEFUL_SLEEP / sleeptime);
	      if (sleepfreq < 1) sleepfreq = 1;
	    } else {
	      sleeptime /= MEASUREMENT_SIZE;
	      sleepfreq = 1;
	    }
	    System.err.println("aSocketRCTMSleep ("+name+"): Adjusted sleeptime to "+sleeptime+", sleepfreq "+sleepfreq);
	  }

	  t1 = System.currentTimeMillis();
	  num_events = 0;
	}

      }
    }

  }

}

