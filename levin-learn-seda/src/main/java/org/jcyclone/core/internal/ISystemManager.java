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

import org.jcyclone.core.stage.IStage;

/**
 * ISystemManager is an internal interface allowing modules
 * to access systemwide features. For now this allows a module to
 * access, create, and destroy thread managers. It also allows a
 * module to create a stage with its own stage wrapper.
 */
public interface ISystemManager {

	/**
	 * Get the default scheduler.
	 */
	IScheduler getScheduler();

	/**
	 * Get the scheduler registered under the given name.
	 */
	IScheduler getScheduler(String name);

	/**
	 * Add a scheduler to the system.
	 */
	void addScheduler(String name, IScheduler threadmgr);

	/**
	 * Create a stage from the given stage wrapper.
	 * If 'initAndStart' is true, the stage will be initialized and started immediately.
	 * Returns a handle to the stage.
	 * <p/>
	 * XXX JM: The stage must be loaded!?
	 */
	IStage createStage(IStageWrapper wrapper, boolean initAndStart)
	    throws Exception;

}
