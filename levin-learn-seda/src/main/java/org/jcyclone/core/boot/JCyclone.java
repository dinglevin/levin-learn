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


package org.jcyclone.core.boot;

import org.jcyclone.core.cfg.ISystemConfig;
import org.jcyclone.core.cfg.JCycloneConfig;
import org.jcyclone.core.internal.ISystemManager;
import org.jcyclone.core.stage.IStageManager;
import org.jcyclone.core.stage.JCycloneMgr;

/**
 * This is the top-level class which acts as the "wrapper" and
 * external interface to the JCyclone runtime. By creating a
 * JCyclone object one can embed a JCyclone system in another
 * application. If you wish to run a standalone JCyclone, this
 * can be done from the commandline using the
 * <tt>org.jcyclone.core.boot.Main</tt> class.
 * <p/>
 * <p>In general it is a good idea to have just one JCyclone instance
 * per JVM; multiple instances may interfere with one another in terms
 * of resource allocation and thread scheduling.
 *
 * @author Matt Welsh
 * @see org.jcyclone.core.boot.Main
 * @see JCycloneConfig
 */
public class JCyclone {

	private JCycloneMgr mgr;
	private static JCyclone instance = null;

	/**
	 * Create a new JCyclone with the default configuration and no
	 * initial stages.
	 */
	public JCyclone() throws Exception {
		this(new JCycloneConfig());
	}

	/**
	 * Create a new JCyclone, reading the configuration from the given
	 * file.
	 */
	public JCyclone(String fname) throws Exception {
		this(new JCycloneConfig(fname));
	}

	/**
	 * Create a new JCyclone with the given configuration.
	 */
	public JCyclone(ISystemConfig config) throws Exception {
		if (instance != null) {
			throw new RuntimeException("JCyclone: Error: Only one JCyclone instance can be running at a given time.");
		}
		instance = this;
		mgr = new JCycloneMgr(config);
		mgr.startStages();
	}

	/**
	 * Return a handler to the stage manager for the JCyclone instance.
	 * This interface allows one to create and obtain handles to stages.
	 */
	public IStageManager getManager() {
		return mgr;
	}

	/**
	 * Return a handle to the system manager for the JCyclone instance.
	 * This interface allows one to create stages and schedulers.
	 */
	public ISystemManager getSystemManager() {
		return mgr;
	}

	/**
	 * Returns the currently-running JCyclone instance, if any.
	 * Returns null if no JCyclone is currently running.
	 */
	public static JCyclone getInstance() {
		return instance;
	}

}

