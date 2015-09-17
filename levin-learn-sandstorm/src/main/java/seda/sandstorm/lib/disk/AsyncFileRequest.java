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

import seda.sandstorm.api.EventElement;
import seda.sandstorm.api.EventSink;

/**
 * Abstract base class of I/O requests which can be posted to the AFile
 * enqueue() methods.
 *
 * @author Matt Welsh
 * @see AsyncFileReadRequest
 * @see AsyncFileWriteRequest
 * @see AsyncFileSeekRequest
 * @see AsyncFileFlushRequest
 * @see AsyncFileCloseRequest
 */
public abstract class AsyncFileRequest implements EventElement {
    private AsyncFile asyncFile;
    private EventSink completionQueue;

    protected AsyncFileRequest(EventSink completionQueue) {
        this.completionQueue = completionQueue;
    }

    protected AsyncFileRequest(AsyncFile asyncFile, EventSink completionQueue) {
        this.asyncFile = asyncFile;
        this.completionQueue = completionQueue;
    }

    AsyncFile getAsyncFile() {
        return asyncFile;
    }
    
    void setAsyncFile(AsyncFile asyncFile) {
        this.asyncFile = asyncFile;
    }

    AsyncFileImpl getImpl() {
        return asyncFile.getImpl();
    }

    EventSink getCompletionQueue() {
        return completionQueue;
    }

    void complete(EventElement event) {
        if (completionQueue != null) {
            completionQueue.enqueueLossy(event);
        }
    }
}
