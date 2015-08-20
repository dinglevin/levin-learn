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

package org.jcyclone.core.stage;

import org.jcyclone.core.cfg.IConfigData;
import org.jcyclone.core.cfg.ISystemConfig;
import org.jcyclone.core.internal.IStageWrapper;
import org.jcyclone.core.internal.MonitoredSink;
import org.jcyclone.core.queue.ISink;

/**
 * A Stage is a basic implementation of StageIF for application-level
 * stages.
 *
 * @author Matt Welsh
 */
public class Stage implements IStage {

	private String name;
	private IStageWrapper wrapper;
	private ISink mainsink;

	// If true, instantate a SinkProxy for the stage's event queue
	// when batchControllor or rtController are enabled. This should
	// be obsolete; older implementations of these controllers relied
	// on the proxy, but it's no longer needed.
	private static final boolean ENABLE_SINK_PROXY = false;

	/**
	 * Create a Stage with the given name, wrapper, and sink.
	 */
	public Stage(String name, IStageWrapper wrapper, ISink mainsink, IConfigData config) {
		this.name = name;
		this.wrapper = wrapper;

		ISystemConfig cf = config.getManager().getConfig();
		this.mainsink = mainsink;

		if (ENABLE_SINK_PROXY &&
		    (cf.getBoolean("global.batchController.enable") ||
		    cf.getBoolean("global.rtController.enable"))) {
			this.mainsink = new MonitoredSink((ISink) mainsink, config.getManager(), wrapper);
		}
	}

	/**
	 * Create a Stage with the given name and wrapper, with no sink.
	 * This is used only for specialized stages.
	 */
	public Stage(String name, IStageWrapper wrapper) {
		this.name = name;
		this.wrapper = wrapper;
	}

	/**
	 * Return the name of this stage.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Return the event sink.
	 */
	public ISink getSink() {
		return (ISink) mainsink;
	}

	/**
	 * Return the stage wrapper for this stage.
	 */
	public IStageWrapper getWrapper() {
		return wrapper;
	}

	/**
	 * Destroy this stage.
	 */
	public void destroy() {
		// nothing to do really, StageWrapper takes care of everything
	}

}

