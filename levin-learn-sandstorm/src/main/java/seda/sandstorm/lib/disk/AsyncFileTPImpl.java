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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import seda.sandstorm.api.BadEventElementException;
import seda.sandstorm.api.EventElement;
import seda.sandstorm.api.EventQueue;
import seda.sandstorm.api.EventSink;
import seda.sandstorm.api.SinkClosedException;
import seda.sandstorm.api.SinkException;
import seda.sandstorm.core.EventQueueImpl;

/**
 * This is an implementation of AFile which uses a pool of threads which perform
 * blocking I/O (through the java.io.RandomAccessFile class) on files. This is a
 * portable implementation but is not intended to be high-performance.
 *
 * @author Matt Welsh
 * @see AsyncFile
 */
class AsyncFileTPImpl extends AsyncFileImpl implements EventElement {
    private File f;
    RandomAccessFile raf;
    private AsyncFile afile;
    private AsyncFileTPTM threadManager;
    private EventSink completionQueue;
    private EventQueueImpl eventQueue;
    private boolean readOnly;
    private boolean closed;

    /**
     * Create an AFileTPIMpl with the given AFile, filename, completion queue,
     * create/readOnly flags, and Thread Manager.
     */
    AsyncFileTPImpl(AsyncFile afile, String fname, EventSink completionQueue, boolean create,
            boolean readOnly, AsyncFileTPTM threadManager) throws IOException {
        this.afile = afile;
        this.threadManager = threadManager;
        this.completionQueue = completionQueue;
        this.readOnly = readOnly;

        eventQueue = new EventQueueImpl("async.file");

        f = new File(fname);
        if (!f.exists() && !create) {
            throw new FileNotFoundException("File not found: " + fname);
        }
        if (f.isDirectory()) {
            throw new FileIsDirectoryException("Is a directory: " + fname);
        }

        if (readOnly) {
            raf = new RandomAccessFile(f, "r");
        } else {
            raf = new RandomAccessFile(f, "rw");
        }
        closed = false;
    }

    /**
     * Enqueues the given request (which must be an AFileRequest) to the file.
     */
    public void enqueue(EventElement req) throws SinkException {
        AsyncFileRequest areq = (AsyncFileRequest) req;
        if (closed) {
            throw new SinkClosedException("Sink is closed");
        }
        if (readOnly && (areq instanceof AsyncFileWriteRequest)) {
            throw new BadEventElementException("Cannot enqueue write request for read-only file", areq);
        }
        areq.setAsyncFile(afile);
        try {
            eventQueue.enqueue(areq);
        } catch (SinkException se) {
            throw new InternalError("Failed to enqueue event: " + req);
        }
        if (eventQueue.size() == 1) {
            threadManager.fileReady(this);
        }
    }

    /**
     * Enqueues the given request (which must be an AFileRequest) to the file.
     */
    public boolean enqueueLossy(EventElement req) {
        AsyncFileRequest areq = (AsyncFileRequest) req;
        if (closed || (readOnly && (areq instanceof AsyncFileWriteRequest))) {
            return false;
        }
        areq.setAsyncFile(afile);
        try {
            eventQueue.enqueue(areq);
        } catch (SinkException se) {
            throw new InternalError("AFileTPImpl.enqueue got SinkException");
        }
        if (eventQueue.size() == 1) {
            threadManager.fileReady(this);
        }
        return true;
    }

    /**
     * Enqueues the given requests (which must be AFileRequests) to the file.
     */
    public void enqueueMany(EventElement[] elements) throws SinkException {
        if (closed) {
            throw new SinkClosedException("Sink is closed");
        }
        for (int i = 0; i < elements.length; i++) {
            enqueue(elements[i]);
        }
    }

    /**
     * Return information on the properties of the file.
     */
    AsyncFileStat stat() {
        AsyncFileStat s = new AsyncFileStat();
        s.afile = afile;
        s.isDirectory = f.isDirectory();
        s.canRead = f.canRead();
        s.canWrite = f.canWrite();
        s.length = f.length();
        return s;
    }

    /**
     * Close the file after all enqueued requests have completed. Disallows any
     * additional requests to be enqueued on this file. A SinkClosedEvent will
     * be posted on the file's completion queue when the close is complete.
     */
    public void close() {
        enqueueLossy(new AsyncFileCloseRequest(afile, completionQueue));
        closed = true;
    }

    /**
     * Causes a SinkFlushedEvent to be posted on the file's completion queue
     * when all pending requests have completed.
     */
    public void flush() {
        enqueueLossy(new AsyncFileFlushRequest(afile, completionQueue));
    }

    /**
     * Return the per-file event queue.
     */
    EventQueue getQueue() {
        return eventQueue;
    }
}
