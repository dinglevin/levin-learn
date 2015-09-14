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

package seda.sandStorm.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import seda.sandStorm.api.ConfigData;
import seda.sandStorm.api.EventHandler;
import seda.sandStorm.api.ManagerIF;
import seda.sandStorm.api.NoSuchStageException;
import seda.sandStorm.api.ProfilableIF;
import seda.sandStorm.api.ProfilerIF;
import seda.sandStorm.api.SignalMgrIF;
import seda.sandStorm.api.Stage;
import seda.sandStorm.api.StageNameAlreadyBoundException;
import seda.sandStorm.api.StagesInitializedSignal;
import seda.sandStorm.api.internal.StageWrapper;
import seda.sandStorm.api.internal.SystemManagerIF;
import seda.sandStorm.api.internal.ThreadManagerIF;
import seda.sandStorm.lib.aDisk.AFileMgr;
import seda.sandStorm.lib.aSocket.SocketMgr;
import seda.sandStorm.main.SandstormConfig;
import seda.sandStorm.main.StageDescr;

/**
 * This class provides management functionality for the Sandstorm runtime
 * system. It is responsible for initializing the system, creating and
 * registering stages and thread managers, and other administrative functions.
 * Stages and thread managers can interact with this class through the ManagerIF
 * and SystemManagerIF interfaces; this class should not be used directly.
 *
 * @author Matt Welsh
 * @see seda.sandStorm.api.ManagerIF
 * @see seda.sandStorm.api.internal.SystemManagerIF
 * 
 */
public class SandStormMgr implements ManagerIF, SystemManagerIF {
    private static final Logger LOGGER = LoggerFactory.getLogger(SandStormMgr.class);

    private ThreadManagerIF defaulttm;
    private Map<String, ThreadManagerIF> tmtbl;

    private SandstormConfig mgrconfig;
    private Map<String, StageWrapper> stagetbl;
    private List<StageWrapper> stagestoinit;
    private SandStormProfiler profiler;
    private SignalMgr signalMgr; 

    /**
     * Create a sandStormMgr which reads its configuration from the given file.
     */
    public SandStormMgr(SandstormConfig mgrconfig) throws Exception {
        this.mgrconfig = mgrconfig;

        stagetbl = new HashMap<>();
        tmtbl = new HashMap<>();
        stagestoinit = new ArrayList<>();
        signalMgr = new SignalMgr();

        String dtm = mgrconfig.getString("global.defaultThreadManager");
        if (dtm == null) {
            throw new IllegalArgumentException("No threadmanager specified by configuration");
        }

        if (dtm.equals(SandstormConfig.THREADMGR_TPPTM)) {
            throw new Error("TPPThreadManager is no longer supported.");
            /* defaulttm = new TPPThreadManager(mgrconfig); */
        } else if (dtm.equals(SandstormConfig.THREADMGR_TPSTM)) {
            defaulttm = new TPSThreadManager(this);
        } else if (dtm.equals(SandstormConfig.THREADMGR_AggTPSTM)) {
            throw new Error("AggTPSThreadManager is no longer supported.");
            /* defaulttm = new AggTPSThreadManager(mgrconfig); */
        } else {
            throw new IllegalArgumentException(
                    "Bad threadmanager specified by configuration: " + dtm);
        }

        tmtbl.put("default", defaulttm);

        initializeIO();
        loadInitialStages();
    }

    /**
     * Start the manager.
     */
    public void start() {
        LOGGER.info("Sandstorm: Initializing stages");
        initStages();

        // Let the threads start
        try {
            LOGGER.info("Sandstorm: Waiting for all components to start...");
            Thread.sleep(500);
        } catch (InterruptedException ie) {
            // Ignore
        }

        LOGGER.info("Sandstorm: Ready.");
    }

    /**
     * Stop the manager.
     */
    public void stop() {
        Iterator<Map.Entry<String, ThreadManagerIF>> iterator = tmtbl.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, ThreadManagerIF> entry = iterator.next();
            System.err.println("Sandstorm: Stopping ThreadManager " + entry.getKey());
            entry.getValue().deregisterAll();
        }

        System.err.println("Sandstorm: Shutting down stages");
        destroyStages();
    }

    /**
     * Return a handle to given stage.
     */
    public Stage getStage(String stagename) throws NoSuchStageException {
        if (stagename == null)
            throw new NoSuchStageException("no such stage: null");
        StageWrapper wrapper = (StageWrapper) stagetbl.get(stagename);
        if (wrapper == null)
            throw new NoSuchStageException("no such stage: " + stagename);
        return wrapper.getStage();
    }

    // Initialize the I/O layer
    private void initializeIO() throws Exception {

        // Create profiler even if disabled
        profiler = new SandStormProfiler(this);

        if (mgrconfig.getBoolean("global.profile.enable")) {
            System.err.println("Sandstorm: Starting profiler");
            profiler.start();
        }

        if (mgrconfig.getBoolean("global.aSocket.enable")) {
            System.err.println("Sandstorm: Starting aSocket layer");
            SocketMgr.initialize(this, this);
        }

        if (mgrconfig.getBoolean("global.aDisk.enable")) {
            System.err.println("Sandstorm: Starting aDisk layer");
            AFileMgr.initialize(this, this);
        }
    }

    // Load stages as specified in the SandstormConfig.
    private void loadInitialStages() throws Exception {
        Iterator<StageDescr> iterator = mgrconfig.getStages();
        if (iterator == null) {
            return;
        }
        
        while (iterator.hasNext()) {
            StageDescr descr = iterator.next();
            loadStage(descr);
        }
    }

    /**
     * Return the default thread manager.
     */
    public ThreadManagerIF getThreadManager() {
        return defaulttm;
    }

    /**
     * Return the thread manager with the given name.
     */
    public ThreadManagerIF getThreadManager(String name) {
        return (ThreadManagerIF) tmtbl.get(name);
    }

    /**
     * Add a thread manager with the given name.
     */
    public void addThreadManager(String name, ThreadManagerIF tm) {
        tmtbl.put(name, tm);
    }

    // Load a stage from the given classname with the given config.
    private void loadStage(StageDescr descr) throws Exception {
        String stageName = descr.stageName;
        String className = descr.className;
        ConfigDataImpl config = new ConfigDataImpl(this, descr.initArgs);
        Class<?> theclass = Class.forName(className);
        EventHandler evHandler = (EventHandler) theclass.newInstance();
        
        LOGGER.info("Sandstorm: Loaded " + stageName + " from " + className);

        StageWrapperImpl wrapper = new StageWrapperImpl(this, stageName, evHandler, config, defaulttm, descr.queueThreshold);

        createStage(wrapper, false);
    }

    /**
     * Create a stage with the given name from the given event handler with the
     * given initial arguments.
     */
    public Stage createStage(String stageName, EventHandler evHandler, String initArgs[]) throws Exception {
        ConfigData config = new ConfigDataImpl(this, initArgs);
        if (stagetbl.get(stageName) != null) {
            // Come up with a better (random) name
            stageName = stageName + "-" + stagetbl.size();
        }
        StageWrapper wrapper = new StageWrapperImpl((ManagerIF) this, stageName,
                evHandler, config, defaulttm);

        return createStage(wrapper, true);
    }

    /**
     * Create a stage from the given stage wrapper. If 'initialize' is true,
     * initialize this stage immediately.
     */
    public Stage createStage(StageWrapper wrapper, boolean initialize) throws Exception {
        String name = wrapper.getStage().getName();
        if (stagetbl.get(name) != null) {
            throw new StageNameAlreadyBoundException(
                    "Stage name " + name + " already in use");
        }
        stagetbl.put(name, wrapper);

        if (mgrconfig.getBoolean("global.profile.enable")) {
            profiler.add(wrapper.getStage().getName() + " queueLength",
                    (ProfilableIF) wrapper.getStage().getSink());
        }

        if (initialize) {
            wrapper.init();
        } else {
            stagestoinit.add(wrapper);
        }
        return wrapper.getStage();
    }

    /**
     * Return the system profiler.
     */
    public ProfilerIF getProfiler() {
        return profiler;
    }

    /**
     * Return the system signal manager.
     */
    public SignalMgrIF getSignalMgr() {
        return signalMgr;
    }

    /**
     * Return the SandstormConfig used to initialize this manager. Actually
     * returns a copy of the SandstormConfig; this prevents options from being
     * changed once the system has been initialized.
     */
    public SandstormConfig getConfig() {
        return mgrconfig.getCopy();
    }

    // Initialize all stages
    private void initStages() {
        for (int i = 0; i < stagestoinit.size(); i++) {
            StageWrapper wrapper = stagestoinit.get(i);
            try {
                System.err.println("-- Initializing <" + wrapper.getStage().getName() + ">");
                wrapper.init();
            } catch (Exception ex) {
                System.err.println("Sandstorm: Caught exception initializing stage " + wrapper.getStage().getName() + ": " + ex);
                ex.printStackTrace();
                System.err.println("Sandstorm: Exiting.");
                System.exit(-1);
            }
        }

        signalMgr.trigger(new StagesInitializedSignal());
    }

    // Destroy all stages
    private void destroyStages() {
        Iterator<Map.Entry<String, StageWrapper>> iterator = stagetbl.entrySet().iterator();
        while (iterator.hasNext()) {
            StageWrapper wrapper = iterator.next().getValue();
            try {
                wrapper.destroy();
            } catch (Exception ex) {
                System.err.println("Sandstorm: Caught exception destroying stage " + wrapper.getStage().getName() + ": " + ex);
                ex.printStackTrace();
            }
        }
    }
}
