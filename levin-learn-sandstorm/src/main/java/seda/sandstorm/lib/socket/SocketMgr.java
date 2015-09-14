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

package seda.sandstorm.lib.socket;

import seda.sandstorm.api.EventSink;
import seda.sandstorm.api.ManagerIF;
import seda.sandstorm.api.SinkException;
import seda.sandstorm.api.Stage;
import seda.sandstorm.api.internal.SystemManagerIF;
import seda.sandstorm.api.internal.ThreadManager;
import seda.sandstorm.internal.ConfigDataImpl;
import seda.sandstorm.main.Sandstorm;
import seda.sandstorm.main.SandstormConfig;

/**
 * The aSocketMgr is an internal class used to provide an interface between the
 * Sandstorm runtime and the aSocket library. Applications should not make use
 * of this class.
 *
 * @author Matt Welsh
 */
public class SocketMgr {
    private static ThreadManager aSocketTM, aSocketRCTM;
    private static EventSink read_sink;
    private static EventSink listenSink;
    private static EventSink write_sink;

    private static Object init_lock = new Object();
    private static boolean initialized = false;

    static boolean USE_NIO = false;
    private static SocketImplFactory factory;

    /**
     * Called at startup time by the Sandstorm runtime.
     */
    public static void initialize(ManagerIF mgr, SystemManagerIF sysmgr) throws Exception {

        synchronized (init_lock) {
            SandstormConfig cfg = mgr.getConfig();
            USE_NIO = true;
            System.err.println("aSocket layer using JDK1.4 java.nio package");

            try {
                factory = SocketImplFactory.getFactory();
            } catch (Exception e) {
                throw new RuntimeException("aSocketMgr: Cannot create aSocketImplFactory: " + e);
            }

            aSocketTM = new SocketThreadManager(mgr);
            sysmgr.addThreadManager("aSocket", aSocketTM);

            ReadEventHandler revh = new ReadEventHandler();
            SocketStageWrapper rsw;
            if (cfg.getBoolean("global.aSocket.governor.enable")) {
                aSocketRCTM = new aSocketRCTMSleep(mgr);
                sysmgr.addThreadManager("aSocketRCTM", aSocketRCTM);
                rsw = new SocketStageWrapper("aSocket ReadStage", revh,
                        new ConfigDataImpl(mgr), aSocketRCTM);
            } else {
                rsw = new SocketStageWrapper("aSocket ReadStage", revh,
                        new ConfigDataImpl(mgr), aSocketTM);
            }

            Stage readStage = sysmgr.createStage(rsw, true);
            read_sink = readStage.getSink();

            ListenEventHandler levh = new ListenEventHandler();
            SocketStageWrapper lsw = new SocketStageWrapper(
                    "aSocket ListenStage", levh, new ConfigDataImpl(mgr),
                    aSocketTM);
            Stage listenStage = sysmgr.createStage(lsw, true);
            listenSink = listenStage.getSink();

            WriteEventHandler wevh = new WriteEventHandler();
            SocketStageWrapper wsw = new SocketStageWrapper(
                    "aSocket WriteStage", wevh, new ConfigDataImpl(mgr), aSocketTM);
            Stage writeStage = sysmgr.createStage(wsw, true);
            write_sink = writeStage.getSink();

            initialized = true;
        }
    }

    /**
     * Ensure that the aSocket layer is initialized, in case the library is
     * being used in standalone mode.
     */
    static void init() {
        synchronized (init_lock) {
            // When invoked in standalone mode
            if (!initialized) {
                try {
                    Sandstorm ss = Sandstorm.getSandstorm();
                    if (ss != null) {
                        initialize(ss.getManager(), ss.getSystemManager());
                    } else {
                        SandstormConfig cfg = new SandstormConfig();
                        ss = new Sandstorm(cfg);
                    }
                } catch (Exception e) {
                    System.err.println(
                            "aSocketMgr: Warning: Initialization failed: " + e);
                    e.printStackTrace();
                    return;
                }
            }
        }
    }

    static SocketImplFactory getFactory() {
        return factory;
    }

    static public void enqueueRequest(aSocketRequest req) {
        init();

        if ((req instanceof ATcpWriteRequest)
                || (req instanceof ATcpConnectRequest)
                || (req instanceof ATcpFlushRequest)
                || (req instanceof ATcpCloseRequest)
                || (req instanceof AUdpWriteRequest)
                || (req instanceof AUdpCloseRequest)
                || (req instanceof AUdpFlushRequest)
                || (req instanceof AUdpConnectRequest)
                || (req instanceof AUdpDisconnectRequest)) {

            try {
                write_sink.enqueue(req);
            } catch (SinkException se) {
                System.err.println(
                        "aSocketMgr.enqueueRequest: Warning: Got SinkException "
                                + se);
                System.err.println(
                        "aSocketMgr.enqueueRequest: This is a bug - contact <mdw@cs.berkeley.edu>");
            }

        } else if ((req instanceof ATcpStartReadRequest)
                || (req instanceof AUdpStartReadRequest)) {

            try {
                read_sink.enqueue(req);
            } catch (SinkException se) {
                System.err.println(
                        "aSocketMgr.enqueueRequest: Warning: Got SinkException "
                                + se);
                System.err.println(
                        "aSocketMgr.enqueueRequest: This is a bug - contact <mdw@cs.berkeley.edu>");
            }

        } else if ((req instanceof ATcpListenRequest)
                || (req instanceof ATcpSuspendAcceptRequest)
                || (req instanceof ATcpResumeAcceptRequest)
                || (req instanceof ATcpCloseServerRequest)) {

            try {
                listenSink.enqueue(req);
            } catch (SinkException se) {
                System.err.println(
                        "aSocketMgr.enqueueRequest: Warning: Got SinkException "
                                + se);
                System.err.println(
                        "aSocketMgr.enqueueRequest: This is a bug - contact <mdw@cs.berkeley.edu>");
            }

        } else {
            throw new IllegalArgumentException("Bad request type " + req);
        }
    }
}
