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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import seda.sandstorm.api.Manager;
import seda.sandstorm.api.Profilable;
import seda.sandstorm.api.internal.StageWrapper;
import seda.sandstorm.main.SandstormConfig;

/**
 * ThreadPool is a generic class which provides a thread pool.
 * 
 * @author Matt Welsh
 */
public class ThreadPool implements Profilable {
    private static final Logger LOGGER = LoggerFactory.getLogger(ThreadPool.class);
    
    private StageWrapper stage;
    private Manager mgr;
    private String poolname;
    private ThreadGroup poolThreadGroup;
    private Runnable runnable;
    private List<Thread> threads;
    private List<Thread> stoppedThreads;

    int minThreads, maxThreads;

    private int maxAggregation;
    private int blockTime = 1000;
    private int idleTimeThreshold;
    private AggThrottle aggThrottle;

    /**
     * Create a thread pool for the given stage, manager and runnable, with the
     * thread pool controller determining the number of threads used.
     */
    public ThreadPool(StageWrapper stage, Manager mgr, Runnable runnable) {
        this.stage = stage;
        this.poolname = stage.getStage().getName();
        this.mgr = mgr;
        this.runnable = runnable;

        SandstormConfig config = mgr.getConfig();
        if (config.getBoolean("global.batchController.enable")) {
            aggThrottle = new AggThrottle(stage, mgr);
        } else {
            this.maxAggregation = config.getInt("global.batchController.maxBatch");
        }

        threads = Lists.newArrayList();
        stoppedThreads = Lists.newArrayList();

        // First look for stages.[stageName] options, then global options
        String tag = "stages." + (stage.getStage().getName()) + ".threadPool.";
        String globaltag = "global.threadPool.";

        int initialSize = config.getInt(tag + "initialThreads");
        if (initialSize < 1) {
            initialSize = config.getInt(globaltag + "initialThreads");
            if (initialSize < 1)
                initialSize = 1;
        }
        minThreads = config.getInt(tag + "minThreads");
        if (minThreads < 1) {
            minThreads = config.getInt(globaltag + "minThreads");
            if (minThreads < 1)
                minThreads = 1;
        }
        maxThreads = config.getInt(tag + "maxThreads");
        if (maxThreads < 1) {
            maxThreads = config.getInt(globaltag + "maxThreads");
            if (maxThreads < 1)
                maxThreads = -1; // Infinite
        }

        this.blockTime = config.getInt(tag + "blockTime",
                config.getInt(globaltag + "blockTime", blockTime));
        this.idleTimeThreshold = config.getInt(tag + "sizeController.idleTimeThreshold",
                        config.getInt(globaltag + "sizeController.idleTimeThreshold", blockTime));

        LOGGER.info("TP <" + poolname + ">: initial " + initialSize
                + ", min " + minThreads + ", max " + maxThreads + ", blockTime "
                + blockTime + ", idleTime " + idleTimeThreshold);

        addThreads(initialSize, false);
        mgr.getProfiler().add("ThreadPool <" + poolname + ">", this);
        poolThreadGroup = new ThreadGroup("TP <" + poolname + ">");
    }

    /**
     * Create a thread pool with the given name, manager, runnable, and thread
     * sizing parameters.
     */
    public ThreadPool(StageWrapper stage, Manager mgr, Runnable runnable,
            int initialThreads, int minThreads, int maxThreads, int blockTime,
            int idleTimeThreshold) {
        this.stage = stage;
        this.poolname = stage.getStage().getName();
        this.mgr = mgr;
        this.runnable = runnable;

        SandstormConfig config = mgr.getConfig();
        if (config.getBoolean("global.batchController.enable")) {
            aggThrottle = new AggThrottle(stage, mgr);
        } else {
            this.maxAggregation = config.getInt("global.batchController.maxBatch");
        }

        threads = Lists.newArrayList();
        stoppedThreads = Lists.newArrayList();
        if (initialThreads < 1)
            initialThreads = 1;
        this.minThreads = minThreads;
        if (this.minThreads < 1)
            this.minThreads = 1;
        this.maxThreads = maxThreads;
        // if (this.maxThreads < 1) this.maxThreads = initialThreads;
        this.blockTime = blockTime;
        this.idleTimeThreshold = idleTimeThreshold;

        addThreads(initialThreads, false);
        mgr.getProfiler().add("ThreadPool <" + poolname + ">", this);
        poolThreadGroup = new ThreadGroup("TP <" + poolname + ">");
    }

    /**
     * Create a thread pool with the given name, manager, runnable, and a fixed
     * number of threads.
     */
    public ThreadPool(StageWrapper stage, Manager mgr, Runnable runnable,
            int numThreads) {
        this.stage = stage;
        this.poolname = stage.getStage().getName();
        this.mgr = mgr;
        this.runnable = runnable;

        SandstormConfig config = mgr.getConfig();
        if (config.getBoolean("global.batchController.enable")) {
            aggThrottle = new AggThrottle(stage, mgr);
        } else {
            this.maxAggregation = config.getInt("global.batchController.maxBatch");
        }

        threads = Lists.newArrayList();
        stoppedThreads = Lists.newArrayList();
        maxThreads = minThreads = numThreads;
        addThreads(numThreads, false);
        mgr.getProfiler().add("ThreadPool <" + poolname + ">", this);
        poolThreadGroup = new ThreadGroup("TP <" + poolname + ">");
    }

    /**
     * Start the thread pool.
     */
    public void start() {
        LOGGER.info("TP <" + poolname + ">: Starting " + numThreads() + " threads" +
                ((aggThrottle != null) ? ", batchController enabled" : (", maxBatch=" + maxAggregation)));
        for (Thread thread : threads) {
            thread.start();
        }
    }

    /**
     * Stop the thread pool.
     */
    @SuppressWarnings("deprecation")
    public void stop() {
        poolThreadGroup.stop();
    }

    /**
     * Add threads to this pool.
     */
    void addThreads(int num, boolean start) {
        synchronized (this) {
            int numToAdd;
            if (maxThreads < 0) {
                numToAdd = num;
            } else {
                int numTotal = Math.min(maxThreads, numThreads() + num);
                numToAdd = numTotal - numThreads();
            }
            if ((maxThreads < 0) || (numToAdd < maxThreads)) {
                LOGGER.info("TP <" + poolname + ">: Adding " + numToAdd
                        + " threads to pool, size "
                        + (numThreads() + numToAdd));
            }
            for (int i = 0; i < numToAdd; i++) {
                String name = "TP-" + numThreads() + " <" + poolname + ">";
                Thread thread = new Thread(poolThreadGroup, runnable, name);
                threads.add(thread);
                mgr.getProfiler().getGraphProfiler().addThread(thread, stage);
                if (start)
                    thread.start();
            }
        }
    }

    /**
     * Remove threads from pool.
     */
    void removeThreads(int num) {
        LOGGER.info("TP <" + poolname + ">: Removing " + num + " threads from pool, ");
        synchronized (this) {
            for (int i = 0; (i < num) && (numThreads() > minThreads); i++) {
                Thread t = threads.get(0);
                stopThread(t);
            }
        }
        LOGGER.info("size " + numThreads());
    }

    /**
     * Cause the given thread to stop execution.
     */
    void stopThread(Thread t) {
        synchronized (this) {
            threads.remove(t);
            stoppedThreads.add(t);
        }
        LOGGER.info("TP <" + poolname + ">: stopping thread, size " + numThreads());
    }

    /**
     * Return the number of threads in this pool.
     */
    int numThreads() {
        synchronized (this) {
            return threads.size();
        }
    }

    /**
     * Used by a thread to determine its queue block time.
     */
    public long getBlockTime() {
        return blockTime;
    }

    /**
     * Used by a thread to request its aggregation target from the pool.
     */
    public synchronized int getAggregationTarget() {
        if (aggThrottle != null) {
            return aggThrottle.getAggTarget();
        } else {
            return maxAggregation;
        }
    }

    /**
     * Used by a thread to determine whether it should exit.
     */
    public boolean timeToStop(long idleTime) {
        synchronized (this) {
            if ((idleTime > idleTimeThreshold) && (numThreads() > minThreads)) {
                stopThread(Thread.currentThread());
            }
            if (stoppedThreads.contains(Thread.currentThread()))
                return true;
        }
        return false;
    }

    public String toString() {
        return "TP (size=" + numThreads() + ") for <" + poolname + ">";
    }

    public String getName() {
        return "ThreadPool <" + poolname + ">";
    }

    public int profileSize() {
        return numThreads();
    }

}
