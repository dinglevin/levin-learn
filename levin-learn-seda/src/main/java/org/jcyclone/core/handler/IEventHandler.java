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

package org.jcyclone.core.handler;

import org.jcyclone.core.cfg.IConfigData;
import org.jcyclone.core.queue.IElement;

import java.util.List;

/**
 * An IEventHandler represents an event handler - the basic unit of
 * computation in JCyclone. This is the basic interface which all
 * application stages implement.
 *
 * @author Matt Welsh
 */
public interface IEventHandler {

	/**
	 * Handle the event corresponding to the given IElement.
	 * This method is invoked by the system when a single event is
	 * pending for the event handler.
	 *
	 * @throws EventHandlerException The application may throw an
	 *                               exception to indicate an error condition during event processing.
	 */
	void handleEvent(IElement elem) throws EventHandlerException;

	/**
	 * Handle the events corresponding to the given IElement list.
	 * This method is invoked when multiple events are pending for the
	 * event handler. The application may reorder, filter, or drop
	 * these events if it wishes to do so.
	 * <p/>
	 * Note that the handler should not keep a reference on the given list
	 * after the execution of this method because the list can be
	 * reused by the caller.
	 *
	 * @throws EventHandlerException The application may throw an
	 *                               exception to indicate an error condition during event processing.
	 */
	void handleEvents(List events) throws EventHandlerException;

	/**
	 * Called when an event handler is initialized. This method should
	 * perform any initialization operations as required by the application.
	 *
	 * @param config The set of configuration parameters for the stage.
	 * @throws Exception The EventHandler can indicate an
	 *                   error to the runtime during initialization by throwing an
	 *                   Exception.
	 */
	void init(IConfigData config) throws Exception;

	/**
	 * Called when an event handler is destroyed. This method should
	 * perform any cleanup or shutdown operations as required by the
	 * application before the event handler is removed from the system.
	 *
	 * @throws Exception The EventHandler can indicate an
	 *                   error to the runtime during shutdown by throwing an
	 *                   Exception.
	 */
	void destroy() throws Exception;

}

