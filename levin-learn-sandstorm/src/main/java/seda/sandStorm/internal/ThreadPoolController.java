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

import java.util.List;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import seda.sandstorm.api.Manager;
import seda.sandstorm.api.Profilable;
import seda.sandstorm.api.internal.StageWrapper;
import seda.sandstorm.main.SandstormConfig;

/**
 * The ThreadPoolController is responsible for dynamically adjusting the size of
 * a given ThreadPool.
 * 
 * @author Matt Welsh
 */

public class ThreadPoolController {
    private static final Logger LOGGER = LoggerFactory.getLogger(ThreadPoolController.class);
    
    // Multiple of standard controller delay
    private static final int CONTROLLER_DELAY = 4;

    // Multiple of standard controller delay
    private static final int THROUGHPUT_MEASUREMENT_DELAY = 1;

    // Multiple of standard controller delay
    private static final int AUTO_MAX_DETECT_DELAY = 10;

    // Size of random jump down in number of threads
    private static final int AUTO_MAX_DETECT_RANDOM_JUMP = 4;

    private static final double SMOOTH_CONST = 0.3;

    private Manager mgr;
    private List<TpcClient> controllerClients;

    private boolean autoMaxDetect;
    private Thread controller;
    private int controllerDelay, controllerThreshold;

    public ThreadPoolController(Manager mgr) {
        this.mgr = mgr;
        controllerClients = Lists.newArrayList();

        SandstormConfig config = mgr.getConfig();
        this.controllerDelay = config.getInt("global.threadPool.sizeController.delay");
        this.controllerThreshold = config.getInt("global.threadPool.sizeController.threshold");
        this.autoMaxDetect = config.getBoolean("global.threadPool.sizeController.autoMaxDetect");

        start();
    }

    public ThreadPoolController(Manager mgr, int delay, int threshold) {
        this.mgr = mgr;
        controllerClients = Lists.newArrayList();
        this.controllerDelay = delay;
        SandstormConfig config = mgr.getConfig();
        if (this.controllerDelay == -1) {
            this.controllerDelay = config.getInt("global.threadPool.sizeController.delay");
        }
        this.controllerThreshold = threshold;
        if (this.controllerThreshold == -1) {
            this.controllerThreshold = config.getInt("global.threadPool.sizeController.threshold");
        }

        this.autoMaxDetect = config.getBoolean("global.threadPool.sizeController.autoMaxDetect");
        start();
    }

    /**
     * Register a thread pool with this controller, using the queue threshold
     * specified by the system configuration.
     */
    public void register(StageWrapper stage, ThreadPool tp) {
        SandstormConfig config = mgr.getConfig();
        int thresh = config.getInt("stages." + stage.getStage().getName()
                        + ".threadPool.sizeController.threshold", controllerThreshold);
        controllerClients.add(new TpcClient(stage, tp, null, thresh));
    }

    /**
     * Register a thread pool with this controller, using the queue threshold
     * specified by the system configuration.
     */
    public void register(StageWrapper stage, ThreadPool tp, Profilable metric) {
        controllerClients.add(new TpcClient(stage, tp, metric, controllerThreshold));
    }

    private void start() {
        LOGGER.info("ThreadPoolController: Started, delay "
                + controllerDelay + " ms, threshold " + controllerThreshold
                + ", autoMaxDetect " + autoMaxDetect);
        controller = new Thread(new ControllerThread(), "TPC");
        controller.start();
    }

    /**
     * Internal class representing a single TPC-controlled thread pool.
     */
    class TpcClient {
        private StageWrapper stage;
        private ThreadPool tp;
        private int threshold;
        private Profilable metric;

        int savedThreads, avgThreads;
        long savedTotalEvents;
        double savedThroughput, avgThroughput;
        long last_time, reset_time;

        TpcClient(final StageWrapper stage, ThreadPool tp, Profilable metric, int threshold) {
            this.stage = stage;
            this.tp = tp;
            this.threshold = threshold;
            this.metric = metric;
            if (this.metric == null) {
                this.metric = new Profilable() {
                    public int profileSize() {
                        return stage.getSource().size();
                    }
                };
            }

            savedThreads = tp.numThreads();
            reset_time = last_time = System.currentTimeMillis();

            mgr.getProfiler().add("TPController savedThreads <" + stage.getStage().getName() + ">",
                    new Profilable() {
                        public int profileSize() {
                            return (int) savedThreads;
                        }
                    });
            mgr.getProfiler().add("TPController avgThreads <" + stage.getStage().getName() + ">",
                    new Profilable() {
                        public int profileSize() {
                            return (int) avgThreads;
                        }
                    });
            mgr.getProfiler().add("TPController savedThroughput <" + stage.getStage().getName() + ">",
                    new Profilable() {
                        public int profileSize() {
                            return (int) savedThroughput;
                        }
                    });
            mgr.getProfiler().add("TPController avgThroughput <"+ stage.getStage().getName() + ">",
                    new Profilable() {
                        public int profileSize() {
                            return (int) avgThroughput;
                        }
                    });
        }
    }

    /**
     * Internal class implementing the controller.
     */
    class ControllerThread implements Runnable {

        int adjust_count = 0;
        Random rand;

        ControllerThread() {
            rand = new Random();
        }

        public void run() {
            LOGGER.debug("TP size controller: starting");

            while (true) {
                adjustThreadPools();
                try {
                    Thread.sleep(controllerDelay);
                } catch (InterruptedException ie) {
                    // Ignore
                }
            }
        }

        private void adjustThreadPools() {
            adjust_count++;
            if ((adjust_count % CONTROLLER_DELAY) == 0) {
                for (int i = 0; i < controllerClients.size(); i++) {
                    TpcClient tpc = controllerClients.get(i);
                    int sz = tpc.metric.profileSize();
                    if (sz >= tpc.threshold)
                        tpc.tp.addThreads(1, true);
                }
            }

            if ((LOGGER.isDebugEnabled() || autoMaxDetect) && (adjust_count % THROUGHPUT_MEASUREMENT_DELAY) == 0) {
                long curTime = System.currentTimeMillis();
                for (int i = 0; i < controllerClients.size(); i++) {
                    TpcClient tpc = controllerClients.get(i);
                    StageWrapperImpl sw;
                    try {
                        sw = (StageWrapperImpl) tpc.stage;
                    } catch (ClassCastException se) {
                        // Skip this one
                        continue;
                    }

                    long events = sw.getStats().getTotalEvents();
                    long curEvents = events - tpc.savedTotalEvents;
                    tpc.savedTotalEvents = events;
                    
                    LOGGER.debug("TP <{}> events {} curEvents", tpc.stage.getStage().getName(), events, curEvents);

                    int curThreads = tpc.tp.numThreads();
                    tpc.avgThreads = (int) ((SMOOTH_CONST * curThreads) + ((1.0 - SMOOTH_CONST) * (double) (tpc.avgThreads * 1.0)));

                    double throughput = (curEvents * 1.0) / ((curTime - tpc.last_time) * 1.0e-3);
                    tpc.avgThroughput = (SMOOTH_CONST * throughput) + ((1.0 - SMOOTH_CONST) * (double) (tpc.avgThroughput * 1.0));
                    
                    LOGGER.debug("TP <{}> throughput {}", tpc.stage.getStage().getName(), tpc.avgThroughput);
                    
                    tpc.last_time = curTime;
                }
            }

            if (autoMaxDetect && (adjust_count % AUTO_MAX_DETECT_DELAY) == 0) {
                for (int i = 0; i < controllerClients.size(); i++) {
                    TpcClient tpc = controllerClients.get(i);

                    if (tpc.avgThroughput >= (1.0 * tpc.savedThroughput)) {
                        // Accept new state
                        tpc.savedThreads = tpc.tp.numThreads();
                        tpc.savedThroughput = tpc.avgThroughput;
                        
                        LOGGER.debug("TP controller <{}> Setting new state to threads={} tp={}",
                            tpc.stage.getStage().getName(), tpc.savedThreads, tpc.savedThroughput);
                    } else
                        if (tpc.avgThroughput <= (1.2 * tpc.savedThroughput)) {
                        if (tpc.savedThreads != tpc.tp.numThreads()) {
                            int numThreads = tpc.tp.numThreads();
                            int nt = (int) (rand.nextDouble() * AUTO_MAX_DETECT_RANDOM_JUMP);
                            int newThreads = Math.max(1, tpc.savedThreads - nt);

                            if (LOGGER.isDebugEnabled() || autoMaxDetect)
                                LOGGER.debug("TP controller <{}> Reverting to threads={}/{} stp=",
                                        tpc.stage.getStage().getName(), tpc.savedThreads, newThreads, tpc.savedThroughput);

                            if (newThreads < numThreads) {
                                tpc.tp.removeThreads(numThreads - newThreads);
                            } else if (newThreads > numThreads) {
                                tpc.tp.addThreads(newThreads - numThreads, true);
                            }
                        }
                    }
                }
                return;
            }
        }
    }
}
