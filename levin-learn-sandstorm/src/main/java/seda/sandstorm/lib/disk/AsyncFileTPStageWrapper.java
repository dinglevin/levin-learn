/* 
 * Copyright (c) 2000 by Matt Welsh and The Regents of the University of 
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

package seda.sandstorm.lib.disk;

import seda.sandstorm.api.ConfigData;
import seda.sandstorm.api.EventHandler;
import seda.sandstorm.api.EventSource;
import seda.sandstorm.api.Stage;
import seda.sandstorm.api.internal.ResponseTimeControllerIF;
import seda.sandstorm.api.internal.StageStats;
import seda.sandstorm.api.internal.StageWrapper;
import seda.sandstorm.api.internal.ThreadManager;
import seda.sandstorm.internal.StageImpl;

/**
 * Internal stage wrapper implementation for AFileTPImpl.
 *
 * @author Matt Welsh
 */
class AsyncFileTPStageWrapper implements StageWrapper {
    private String name;
    private Stage stage;
    private EventHandler handler;
    private ConfigData config;
    private ThreadManager tm;

    // This stagewrapper has no (real) event queue: Threads created
    // by AFileTPTM will poll across the per-AFile queues instead.
    // This class is just used for bookkeeping purposes.
    AsyncFileTPStageWrapper(String name, EventHandler handler,
            ConfigData config, ThreadManager tm) {
        this.name = name;
        this.handler = handler;
        this.config = config;
        this.tm = tm;
        this.stage = new StageImpl(name, this, null, config);
        this.config.setStage(this.stage);
    }

    /**
     * Initialize this stage.
     */
    public void init() throws Exception {
        if (handler != null) {
            handler.init(config);
        }

        tm.register(this);
    }

    /**
     * Destroy this stage.
     */
    public void destroy() throws Exception {
        tm.deregister(this);
        if (handler != null)
            handler.destroy();
    }
    
    public String getName() {
        return name;
    }

    /**
     * Return the event handler associated with this stage.
     */
    public EventHandler getEventHandler() {
        return handler;
    }

    /**
     * Return the stage handle for this stage.
     */
    public Stage getStage() {
        return stage;
    }

    /**
     * Return the source from which events should be pulled to pass to this
     * EventHandlerIF.
     */
    public EventSource getSource() {
        // Not used
        return null;
    }

    /**
     * Return the thread manager for this stage.
     */
    public ThreadManager getThreadManager() {
        return tm;
    }

    /** Not implemented. */
    public StageStats getStats() {
        return null;
    }

    /** Not implemented. */
    public ResponseTimeControllerIF getResponseTimeController() {
        return null;
    }

    public String toString() {
        return "AFILETPSW[" + stage.getName() + "]";
    }

}
