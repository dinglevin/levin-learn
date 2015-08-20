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

package org.jcyclone.core.profiler;


/**
 * A IProfiler is responsible for profiling the behavior of the
 * system over time. A snapshot of the size of all registered
 * ProfilableIF objects is taken and forwarded to registered
 * ProfilerHandlerIF objects, which can export the snapshot
 * to a variety of destinations, including files, GUI, etc.
 * <p/>
 * Applications can get a handle to the ProfilerIF by invoking
 * IStageManager.getProfiler().
 *
 * @author Matt Welsh and Jean Morissette
 * @see org.jcyclone.core.stage.IStageManager
 */
public interface IProfiler {

	/**
	 * Returns true if the system is actually being run in profiling mode;
	 * false otherwise.
	 */
	boolean isRunning();

	/**
	 * Add a class to the profile. This will cause the profiler to track
	 * the object's size over time.
	 *
	 * @param name The name of the object as it should appear in the profile.
	 * @param pr   The object to profile.
	 */
	void add(String name, IProfilable pr);

	void remove(String name);

	void addHandler(IProfilerHandler handler);

	void removeHandler(IProfilerHandler handler);

}
