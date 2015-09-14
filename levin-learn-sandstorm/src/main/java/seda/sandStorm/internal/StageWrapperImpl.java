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

package seda.sandstorm.internal;

import seda.sandstorm.api.ConfigData;
import seda.sandstorm.api.EventHandler;
import seda.sandstorm.api.EventSink;
import seda.sandstorm.api.EventSource;
import seda.sandstorm.api.ManagerIF;
import seda.sandstorm.api.Stage;
import seda.sandstorm.api.internal.ResponseTimeControllerIF;
import seda.sandstorm.api.internal.StageStats;
import seda.sandstorm.api.internal.StageWrapper;
import seda.sandstorm.api.internal.ThreadManager;
import seda.sandstorm.core.FiniteQueue;
import seda.sandstorm.core.QueueThresholdPredicate;

/**
 * A StageWrapper is a basic implementation of StageWrapperIF for
 * application-level stages.
 * 
 * @author Matt Welsh
 */

class StageWrapperImpl implements StageWrapper {
    private String name;
    private Stage stage;
    private EventHandler handler;
    private ConfigData config;
    private FiniteQueue eventQ;
    private ThreadManager threadmgr;
    private StageStats stats;
    private ResponseTimeControllerIF rtcon;

    /**
     * Create a StageWrapper with the given name, handler, config data, and
     * thread manager.
     */
    StageWrapperImpl(ManagerIF mgr, String name, EventHandler handler,
            ConfigData config, ThreadManager threadmgr) {
        this.name = name;
        this.handler = handler;
        this.config = config;
        this.threadmgr = threadmgr;
        eventQ = new FiniteQueue(name);
        this.stats = new StageStatsImpl(this);
        this.stage = new StageImpl(name, this, (EventSink) eventQ, config);
        config.setStage(this.stage);
        createRTController(mgr);
    }

    /**
     * Create a StageWrapper with the given name, handler, config data, thread
     * manager, and queue threshold.
     */
    StageWrapperImpl(ManagerIF mgr, String name, EventHandler handler,
            ConfigData config, ThreadManager threadmgr,
            int queueThreshold) {
        this.name = name;
        this.handler = handler;
        this.config = config;
        this.threadmgr = threadmgr;
        this.stats = new StageStatsImpl(this);
        this.rtcon = null;

        eventQ = new FiniteQueue(name);
        QueueThresholdPredicate pred = new QueueThresholdPredicate(eventQ,
                queueThreshold);
        eventQ.setEnqueuePredicate(pred);

        this.stage = new StageImpl(name, this, (EventSink) eventQ, config);
        config.setStage(this.stage);
        createRTController(mgr);
    }

    private void createRTController(ManagerIF mgr) {
        boolean rtControllerEnabled = mgr.getConfig()
                .getBoolean("global.rtController.enable");
        String deftype = mgr.getConfig().getString("global.rtController.type");
        if (mgr.getConfig().getBoolean(
                "stages." + name + ".rtController.enable",
                rtControllerEnabled)) {
            String contype = mgr.getConfig().getString(
                    "stages." + name + ".rtController.type", deftype);
            if (contype == null) {
                this.rtcon = new ResponseTimeControllerDirect(mgr, this);
            } else if (contype.equals("direct")) {
                this.rtcon = new ResponseTimeControllerDirect(mgr, this);
            } else if (contype.equals("mm1")) {
                this.rtcon = new ResponseTimeControllerMM1(mgr, this);
            } else if (contype.equals("pid")) {
                this.rtcon = new ResponseTimeControllerPID(mgr, this);
            } else if (contype.equals("multiclass")) {
                this.rtcon = new ResponseTimeControllerMulticlass(mgr, this);
            } else {
                throw new RuntimeException("StageWrapper <" + name
                        + ">: Bad response time controller type " + contype);
            }
        }
    }

    /**
     * Initialize this stage.
     */
    public void init() throws Exception {
        handler.init(config);
        threadmgr.register(this);
    }

    /**
     * Destroy this stage.
     */
    public void destroy() throws Exception {
        threadmgr.deregister(this);
        handler.destroy();
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
     * Return the set of sources from which events should be pulled to pass to
     * this EventHandlerIF.
     */
    public EventSource getSource() {
        return (EventSource) eventQ;
    }

    /**
     * Return the thread manager which will run this stage.
     */
    public ThreadManager getThreadManager() {
        return threadmgr;
    }

    /**
     * Return execution statistics for this stage.
     */
    public StageStats getStats() {
        return stats;
    }

    /**
     * Return the response time controller, if any.
     */
    public ResponseTimeControllerIF getResponseTimeController() {
        return rtcon;
    }

    public String toString() {
        return "SW[" + stage.getName() + "]";
    }

}
