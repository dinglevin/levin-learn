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

import org.jcyclone.core.cfg.JCycloneConfig;

import java.io.IOException;
import java.util.Date;

/**
 * This class is used to start a JCyclone system from the commandline.
 * The usage is:
 * <pre>
 *   java org.jcyclone.core.boot.Main &lt;configuration file&gt; [initargs]
 * </pre>
 * <p/>
 * A JCyclone can be embedded within an application using the
 * <tt>JCyclone</tt> class.
 *
 * @author Matt Welsh
 * @see JCyclone
 */
public class Main {

	public static final int MAJOR_VERSION = 3;
	public static final int MINOR_VERSION = 0;
	public static final String VERSION_STRING = MAJOR_VERSION + "." + MINOR_VERSION;
	public static final String WELCOME_STRING = "JCyclone v" + VERSION_STRING + " <mdw@cs.berkeley.edu>";

	private static void usage() {
		System.err.println("Usage:");
		System.err.println("\tjava org.jcyclone.core.boot.Main [-profile] <configfile> [initargs]\n");
		System.exit(-1);
	}

	public static void main(String args[]) {

		try {

			Date d = new Date();
			if (args.length < 1) usage();

			System.out.println(WELCOME_STRING);
			System.out.println("  Starting at " + d.toString() + "\n");

			int n;
			boolean PROFILE = false;

			if ((args.length > 1) && (args[0].equals("-profile"))) {
				PROFILE = true;
				n = 1;
			} else {
				n = 0;
			}

			int numinitargs = args.length - n - 1;
			String initargs[] = null;
			if (numinitargs > 0) {
				initargs = new String[numinitargs];
				for (int j = 0; j < numinitargs; j++) {
					initargs[j] = args[n + 1 + j];
				}
			}

			// -profile option overrides configuration file
			JCycloneConfig cfg;
			try {
				cfg = new JCycloneConfig(args[n], initargs);
			} catch (IOException fnfe) {
				System.err.println("Error opening configuration file '" + args[n] + "': " + fnfe);
				fnfe.printStackTrace();
				usage();
				return;
			}
			if (PROFILE) cfg.putBoolean("global.profile.enable", true);
			JCyclone ss = new JCyclone(cfg);

		} catch (Exception e) {
			System.err.println("JCyclone main(): Got exception: " + e);
			e.printStackTrace();
		}

	}
}

