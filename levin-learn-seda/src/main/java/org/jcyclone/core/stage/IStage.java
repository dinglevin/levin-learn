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

import org.jcyclone.core.internal.IStageWrapper;
import org.jcyclone.core.queue.ISink;

/**
 * A IStage represents a handle to an application stage. Applications
 * to not implement StageIF directly; rather, they implement IEventHandler.
 * A IStage is used by an event handler to access other stages and is
 * obtained by a call to IManager.getStage().
 *
 * @author Matt Welsh
 * @see org.jcyclone.core.handler.IEventHandler
 * @see org.jcyclone.core.stage.IStageManager
 */
public interface IStage {

	/**
	 * Return the name of this stage.
	 */
	String getName();

	/**
	 * Return the event sink for this stage.
	 */
	ISink getSink();

	/**
	 * Return the stage wrapper associated with this stage.
	 * This method provide an entry point in JCyclone internal
	 * and should be used carefully.
	 */
	IStageWrapper getWrapper();

}

