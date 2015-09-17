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

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;

import seda.sandstorm.api.EventElement;
import seda.sandstorm.api.EventSource;
import seda.sandstorm.api.Manager;
import seda.sandstorm.api.SingleThreadedEventHandlerIF;
import seda.sandstorm.api.internal.ResponseTimeController;
import seda.sandstorm.api.internal.StageWrapper;
import seda.sandstorm.api.internal.ThreadManager;
import seda.sandstorm.main.SandstormConfig;

/**
 * TPSThreadManager provides a threadpool-per-source-per-stage thread manager
 * implementation.
 * 
 * @author Matt Welsh
 */

public class TPSThreadManager implements ThreadManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(TPSThreadManager.class);
    
    protected Manager mgr;
    protected SandstormConfig config;
    protected Map<StageRunnable, StageWrapper> srTbl;
    protected ThreadPoolController sizeController;

    public TPSThreadManager(Manager mgr) {
        this(mgr, true);
    }

    public TPSThreadManager(Manager mgr, boolean initialize) {
        this.mgr = mgr;
        this.config = mgr.getConfig();

        if (initialize) {
            if (config.getBoolean("global.threadPool.sizeController.enable")) {
                sizeController = new ThreadPoolController(mgr);
            }
            srTbl = Maps.newHashMap();
        }
    }

    /**
     * Register a stage with this thread manager.
     */
    public void register(StageWrapper stage) {
        // Create a threadPool for the stage
        StageRunnable sr = new StageRunnable(stage);
        srTbl.put(sr, stage);
    }

    /**
     * Deregister a stage with this thread manager.
     */
    public void deregister(StageWrapper stage) {
        for (Map.Entry<StageRunnable, StageWrapper> entry : srTbl.entrySet()) {
            StageRunnable stageRunnable = entry.getKey();
            StageWrapper existingStage = entry.getValue();
            if (existingStage == stage) {
                stageRunnable.threadPool.stop();
                srTbl.remove(stageRunnable);
            }
        }
    }

    /**
     * Stop the thread manager and all threads managed by it.
     */
    public void deregisterAll() {
        for (Map.Entry<StageRunnable, StageWrapper> entry : srTbl.entrySet()) {
            StageRunnable stageRunnable = entry.getKey();
            stageRunnable.threadPool.stop();
            srTbl.remove(stageRunnable);
        }
    }

    /**
     * Internal class representing the Runnable for a single stage.
     */
    public class StageRunnable implements Runnable {
        protected ThreadPool threadPool;
        protected StageWrapper wrapper;
        protected EventSource source;
        protected String name;
        protected ResponseTimeController rtController = null;
        protected boolean firstToken = false;
        protected int aggTarget = -1;

        protected StageRunnable(StageWrapper wrapper, ThreadPool tp) {
            this.wrapper = wrapper;
            this.threadPool = tp;
            this.source = wrapper.getSource();
            this.name = wrapper.getStage().getName();

            if (tp != null) {
                if (sizeController != null) {
                    // The sizeController is globally enabled -- has the user disabled it for this stage?
                    String val = config.getString("stages." + this.name + ".threadPool.sizeController.enable");
                    if ((val == null) || val.equals("true") || val.equals("TRUE")) {
                        sizeController.register(wrapper, tp);
                    }
                }
            }
            this.rtController = wrapper.getResponseTimeController();

            if (tp != null)
                tp.start();
        }

        protected StageRunnable(StageWrapper wrapper) {
            this.wrapper = wrapper;
            this.source = wrapper.getSource();
            this.name = wrapper.getStage().getName();

            // Create a threadPool for the stage
            if (wrapper.getEventHandler() instanceof SingleThreadedEventHandlerIF) {
                threadPool = new ThreadPool(wrapper, mgr, this, 1);
            } else {
                threadPool = new ThreadPool(wrapper, mgr, this);
            }

            if (sizeController != null) {
                // The sizeController is globally enabled -- has the user disabled it for this stage?
                String val = config.getString("stages." + this.name + ".threadPool.sizeController.enable");
                if ((val == null) || val.equals("true") || val.equals("TRUE")) {
                    sizeController.register(wrapper, threadPool);
                }
            }
            this.rtController = wrapper.getResponseTimeController();

            threadPool.start();
        }

        public void run() {
            int blockTime;
            long t1, t2;
            long tstart = 0, tend = 0;
            boolean isFirst = false;

            LOGGER.debug("{}: starting, source is {}", name, source);

            t1 = System.currentTimeMillis();

            while (true) {
                synchronized (this) {
                    if (firstToken == false) {
                        firstToken = true;
                        isFirst = true;
                    }
                }

                try {

                    blockTime = (int) threadPool.getBlockTime();
                    aggTarget = threadPool.getAggregationTarget();

                    LOGGER.trace("{}: Doing blocking dequeue for {}", name, wrapper);

                    EventElement fetched[];
                    if (aggTarget == -1) {
                        LOGGER.trace("TPSTM <{}> dequeue (aggTarget -1)", this.name);
                        fetched = source.blockingDequeueAll(blockTime);
                    } else {
                        LOGGER.trace("TPSTM <{}> dequeue (aggTarget {})", this.name, aggTarget);
                        fetched = source.blockingDequeue(blockTime, aggTarget);
                    }

                    if (fetched == null) {
                        t2 = System.currentTimeMillis();
                        if (threadPool.timeToStop(t2 - t1)) {
                            LOGGER.debug("{}: Exiting", name);
                            if (isFirst) {
                                synchronized (this) {
                                    firstToken = false;
                                }
                            }
                            return;
                        }
                        continue;
                    }

                    t1 = System.currentTimeMillis();

                    LOGGER.debug("{}: Got {} elements for {}", name, fetched.length, wrapper);

                    /* Process events */
                    tstart = System.currentTimeMillis();
                    wrapper.getEventHandler().handleEvents(fetched);
                    tend = System.currentTimeMillis();

                    /* Record service rate */
                    ((StageWrapperImpl) wrapper).getStats().recordServiceRate(fetched.length, tend - tstart);

                    /* Run response time controller controller */
                    if (rtController != null) {
                        if (rtController instanceof ResponseTimeControllerMM1) {
                            ((ResponseTimeControllerMM1) rtController)
                                    .adjustThreshold(fetched, tstart, tend,
                                            isFirst, threadPool.numThreads());
                        } else {
                            rtController.adjustThreshold(fetched, tend - tstart);
                        }
                    }

                    if (threadPool.timeToStop(0)) {
                        LOGGER.debug("{}: Exiting", name);
                        if (isFirst) {
                            synchronized (this) {
                                firstToken = false;
                            }
                        }
                        return;
                    }

                    Thread.yield();
                } catch (Exception e) {
                    LOGGER.error("TPSThreadManager: appThread [" + name + "] got exception ", e);
                }
            }
        }
    }
}
