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

import org.jcyclone.core.handler.IEventHandler;
import org.jcyclone.core.queue.ISource;
import org.jcyclone.core.rtc.IAdmissionControlledSink;
import org.jcyclone.core.rtc.IResponseTimeController;
import org.jcyclone.core.stage.IStage;

/**
 * A StageWrapperIF is the internal representation for an application
 * stage - an event handler coupled with a set of queues.
 *
 * @author Matt Welsh and Jean Morissette
 */
public interface IStageWrapper {
	public static final int LOADED = 1;
	public static final int PROGRAMMED = 2;
	public static final int INITIALIZED = 3;
	public static final int STARTED = 4;

	/**
	 * Return the IStage for this stage.
	 */
	IStage getStage();

	/**
	 * Return the name of this stage.
	 */
	String getName();

	/**
	 * Return the event handler associated with this stage.
	 */
	IEventHandler getEventHandler();

	/**
	 * Return the source from which events should be pulled to
	 * pass to this stage event handler.
	 */
	ISource getSource();

	/**
	 * Return the thread manager which will run this stage.
	 */
	IScheduler getThreadManager();

	/**
	 * Return a IStageStats which records and manages performance
	 * statistics for this stage.
	 */
	IStageStats getStats();

	/**
	 * Return the response time controller for this stage.
	 */
	IResponseTimeController getResponseTimeController();

	/**
	 * Return the batch sorter for this stage.
	 */
	IBatchSorter getBatchSorter();

	/**
	 * Set the batch sorter for this stage.
	 */
	void setBatchSorter(IBatchSorter sorter);

	IAdmissionControlledSink getSink();

	int getLifecycleLevel();

	void program() throws Exception;

	/**
	 * Initialize this stage.
	 */
	void init() throws Exception;

	void start() throws Exception;

	void stop() throws Exception;

	/**
	 * Destroy this stage.
	 */
	void destroy() throws Exception;

	void deprogram() throws Exception;

}

