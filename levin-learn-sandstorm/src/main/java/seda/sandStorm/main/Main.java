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

package seda.sandStorm.main;

import java.io.IOException;
import java.util.Date;

import static seda.sandStorm.internal.SandStormConst.WELCOME_STRING;

/**
 * This class is used to start a Sandstorm system from the commandline. The
 * usage is:
 * 
 * <pre>
 *   java seda.sandStorm.main.Main &lt;configuration file&gt; [initargs]
 * </pre>
 *
 * A Sandstorm can be embedded within an application using the
 * <tt>Sandstorm</tt> class.
 *
 * @author Matt Welsh
 * @see Sandstorm
 *
 */
public class Main {
    private static void usage() {
        System.err.println("Usage:");
        System.err.println("\tjava seda.sandStorm.main.Main [-profile] <configfile> [initargs]\n");
        System.exit(-1);
    }

    public static void main(String args[]) {
        try {
            if (args.length < 1) {
                usage();
            }

            System.out.println(WELCOME_STRING);
            System.out.println("  Starting at " + new Date() + "\n");

            int n = 0;
            boolean PROFILE = false;
            if ((args.length > 1) && (args[0].equals("-profile"))) {
                PROFILE = true;
                n++;
            }

            int numInitArgs = args.length - n - 1;
            String initArgs[] = null;
            if (numInitArgs > 0) {
                initArgs = new String[numInitArgs];
                for (int j = 0; j < numInitArgs; j++) {
                    initArgs[j] = args[n + 1 + j];
                }
            }

            // -profile option overrides configuration file
            SandstormConfig sscfg;
            try {
                sscfg = new SandstormConfig(args[n], initArgs);
            } catch (IOException fnfe) {
                System.err.println("Error opening configuration file '" + args[n] + "': " + fnfe);
                fnfe.printStackTrace();
                usage();
                return;
            }
            
            if (PROFILE) {
                sscfg.putBoolean("global.profile.enable", true);
            }
            
            new Sandstorm(sscfg);
        } catch (Exception e) {
            System.err.println("Sandstorm.main(): Got exception: " + e);
            e.printStackTrace();
        }
    }
}
