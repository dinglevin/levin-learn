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

import java.io.IOException;
import java.io.RandomAccessFile;

import seda.sandstorm.api.EventElement;
import seda.sandstorm.api.EventSource;
import seda.sandstorm.api.Manager;
import seda.sandstorm.api.Profilable;
import seda.sandstorm.api.SinkClosedEvent;
import seda.sandstorm.api.SinkException;
import seda.sandstorm.api.SinkFlushedEvent;
import seda.sandstorm.api.Stage;
import seda.sandstorm.api.internal.StageWrapper;
import seda.sandstorm.api.internal.SystemManager;
import seda.sandstorm.api.internal.ThreadManager;
import seda.sandstorm.core.BufferEvent;
import seda.sandstorm.core.EventQueueImpl;
import seda.sandstorm.internal.ConfigDataImpl;
import seda.sandstorm.internal.TPSThreadManager;
import seda.sandstorm.internal.ThreadPool;
import seda.sandstorm.internal.ThreadPoolController;
import seda.sandstorm.main.SandstormConfig;

/**
 * This is the ThreadManager implementation for AFileTPImpl. It manages a pool
 * of threads which perform blocking I/O on disk files; this is a portable
 * implementation and is not meant to be high performance.
 *
 * @author Matt Welsh
 */
class AsyncFileTPTM extends TPSThreadManager implements ThreadManager, Profilable {

    private static final boolean DEBUG = false;

    // Global queue for files with pending entries
    private EventQueueImpl fileQ;
    // Count of outstanding file requests, derived from length of
    // queue of each file on fileQ
    private int numOutstandingRequests;

    // Maximum number of consecutive requests to service per file
    private static final int MAX_REQUESTS_PER_FILE = 10;

    AsyncFileTPTM(Manager mgr, SystemManager sysmgr) throws Exception {
        super(mgr, false);

        if (DEBUG)
            System.err.println("AFileTPTM: Created");

        if (config
                .getBoolean("global.aDisk.threadPool.sizeController.enable")) {
            sizeController = new ThreadPoolController(mgr,
                    config.getInt(
                            "global.aDisk.threadPool.sizeController.delay"),
                    config.getInt(
                            "global.aDisk.threadPool.sizeController.threshold"));
        }

        fileQ = new EventQueueImpl("async.file.tps");
        numOutstandingRequests = 0;
        sysmgr.addThreadManager("AFileTPTM", this);
        AsyncFileTPStageWrapper sw = new AsyncFileTPStageWrapper("AFileTPTM Stage",
                null, new ConfigDataImpl(mgr), this);
        Stage theStage = sysmgr.createStage(sw, true);

        if (mgr.getProfiler() != null) {
            mgr.getProfiler().add("AFileTPTM outstanding reqs", this);
        }
    }

    /**
     * Register a stage with this thread manager.
     */
    public void register(StageWrapper stage) {
        // Create a single threadPool - only one stage registered with us
        AFileTPThread at = new AFileTPThread((AsyncFileTPStageWrapper) stage);
        SandstormConfig config = mgr.getConfig();
        ThreadPool tp = new ThreadPool(stage, mgr, at,
                config.getInt("global.aDisk.threadPool.initialThreads"),
                config.getInt("global.aDisk.threadPool.minThreads"),
                config.getInt("global.aDisk.threadPool.maxThreads"),
                config.getInt("global.threadPool.blockTime"), config.getInt(
                        "global.threadPool.sizeController.idleTimeThreshold"));
        at.registerTP(tp);
        // Use numOutstandingRequests as metric
        if (sizeController != null)
            sizeController.register(stage, tp, this);
        tp.start();
    }

    /**
     * Indicate that a file has pending events.
     */
    public void fileReady(AsyncFileTPImpl impl) {
        try {
            fileQueueEntry fqe = new fileQueueEntry(impl);
            fileQ.enqueue(fqe);
            synchronized (fileQ) {
                numOutstandingRequests += fqe.size;
            }
        } catch (SinkException se) {
            throw new InternalError(
                    "AFileTPTM.fileReady() got SinkException -- this should not happen, please contact <mdw@cs.berkeley.edu>");
        }
    }

    // Return the number of outstanding elements, for profiling
    public int profileSize() {
        return numOutstandingRequests;
    }

    // Used to keep track of number of elements on fileQ
    class fileQueueEntry implements EventElement {
        AsyncFileTPImpl impl;
        int size;

        fileQueueEntry(AsyncFileTPImpl impl) {
            this.impl = impl;
            this.size = ((EventSource) impl.getQueue()).size();
        }
    }

    /**
     * Internal class representing a single AFileTPTM-managed thread.
     */
    class AFileTPThread extends TPSThreadManager.StageRunnable
            implements Runnable {

        AFileTPThread(AsyncFileTPStageWrapper wrapper) {
            super(wrapper, null);
        }

        public void registerTP(ThreadPool tp) {
            this.tp = tp;
        }

        public void run() {
            int blockTime;
            long t1, t2;

            if (DEBUG)
                System.err.println(name + ": starting");

            t1 = System.currentTimeMillis();

            while (true) {

                try {

                    blockTime = (int) tp.getBlockTime();

                    AsyncFileTPImpl impl;
                    fileQueueEntry fqe = (fileQueueEntry) fileQ
                            .blockingDequeue(blockTime);
                    if (fqe == null) {
                        t2 = System.currentTimeMillis();
                        if (tp.timeToStop(t2 - t1)) {
                            if (DEBUG)
                                System.err.println(name + ": Exiting");
                            return;
                        }
                        continue;
                    }
                    t1 = System.currentTimeMillis();

                    impl = fqe.impl;
                    synchronized (fileQ) {
                        numOutstandingRequests -= fqe.size;
                    }

                    int n = 0;

                    while (n < MAX_REQUESTS_PER_FILE) {
                        AsyncFileRequest req = (AsyncFileRequest) impl
                                .getQueue().dequeue();
                        if (req == null)
                            break;
                        processRequest(req);
                        n++;
                    }
                    // If events still pending, place back on file queue
                    if (((EventSource) impl.getQueue()).size() != 0)
                        fileReady(impl);

                    Thread.yield();
                } catch (Exception e) {
                    System.err.println(name + ": got exception " + e);
                    e.printStackTrace();
                }
            }
        }

        private void processRequest(AsyncFileRequest req) {
            if (DEBUG)
                System.err.println(name + " processing request: " + req);

            // Read request
            if (req instanceof AsyncFileReadRequest) {
                AsyncFileReadRequest rreq = (AsyncFileReadRequest) req;
                AsyncFileTPImpl impl = (AsyncFileTPImpl) rreq.getImpl();
                RandomAccessFile raf = impl.raf;
                BufferEvent buf = rreq.buf;
                try {
                    int c = raf.read(buf.data, buf.offset, buf.size);
                    if (c == -1) {
                        req.complete(new AsyncFileEOFReached(req));
                    } else if (c < buf.size) {
                        // This can occur if buf.size is less than the size of
                        // the file
                        req.complete(new AsyncFileIOCompleted(req, c));
                        req.complete(new AsyncFileEOFReached(req));
                    } else {
                        req.complete(new AsyncFileIOCompleted(req, buf.size));
                    }
                } catch (IOException ioe) {
                    req.complete(new AsyncFileIOExceptionOccurred(req, ioe));
                }

                // Write request
            } else if (req instanceof AsyncFileWriteRequest) {
                AsyncFileWriteRequest wreq = (AsyncFileWriteRequest) req;
                AsyncFileTPImpl impl = (AsyncFileTPImpl) wreq.getImpl();
                RandomAccessFile raf = impl.raf;
                BufferEvent buf = wreq.buf;
                try {
                    raf.write(buf.data, buf.offset, buf.size);
                    req.complete(new AsyncFileIOCompleted(req, buf.size));
                } catch (IOException ioe) {
                    req.complete(new AsyncFileIOExceptionOccurred(req, ioe));
                }

                // Seek request
            } else if (req instanceof AsyncFileSeekRequest) {
                AsyncFileSeekRequest sreq = (AsyncFileSeekRequest) req;
                AsyncFileTPImpl impl = (AsyncFileTPImpl) sreq.getImpl();
                RandomAccessFile raf = impl.raf;
                try {
                    raf.seek(sreq.offset);
                } catch (IOException ioe) {
                    req.complete(new AsyncFileIOExceptionOccurred(req, ioe));
                }

                // Close request
            } else if (req instanceof AsyncFileCloseRequest) {
                AsyncFileCloseRequest creq = (AsyncFileCloseRequest) req;
                AsyncFileTPImpl impl = (AsyncFileTPImpl) creq.getImpl();
                RandomAccessFile raf = impl.raf;
                try {
                    raf.close();
                } catch (IOException ioe) {
                    req.complete(new AsyncFileIOExceptionOccurred(req, ioe));
                }
                req.complete(new SinkClosedEvent(req.afile));

                // Flush request
            } else if (req instanceof AsyncFileFlushRequest) {
                // Don't know how to flush an RAF
                req.complete(new SinkFlushedEvent(req.afile));

            } else {
                throw new Error(
                        "AFileTPTM.AFileTPThread.processRequest got bad request: "
                                + req);
            }

        }
    }

}
