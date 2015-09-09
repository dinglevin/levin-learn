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

import java.util.*;

/**
 * This is an internal class used to represent the configuration parameters for
 * a stage.
 * 
 * @author Matt Welsh
 * @see SandstormConfig
 *
 */
public class StageDescr {
    /** The name of the stage. */
    public final String stageName;

    /** The fully-qualified class name of the stage's event handler. */
    public final String className;

    /** The initial arguments to the stage. */
    public final Map<String, String> initArgs;

    /**
     * The stage's event queue threshold. -1 indicates an infinite threshold.
     */
    public final int queueThreshold;

    public StageDescr(String stageName, String className, Map<String, String> initArgs, int queueThreshold) {
        this.stageName = stageName;
        this.className = className;
        this.initArgs = initArgs;
        this.queueThreshold = queueThreshold;
    }
    
    public StageDescr(String stageName, String className, Map<String, String> initArgs) {
        this(stageName, className, initArgs, -1);
    }
}
