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

package seda.sandStorm.lib.aSocket;

import java.io.IOException;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import seda.sandStorm.api.ConfigDataIF;
import seda.sandStorm.api.EventHandler;
import seda.sandStorm.api.QueueElementIF;

/**
 * Internal event handler for socket listen events.
 */
class ListenEventHandler extends SocketEventHandler implements EventHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ListenEventHandler.class);

    ListenEventHandler() {
    }

    public void init(ConfigDataIF config) {
    }

    public void destroy() {
    }

    private void processAccept(ListenSockState lss) throws IOException {
        LOGGER.debug("ListenEventHandler: processAccept called");

        int numAccepted = 0;

        // Try to do as many accepts as we can in one go
        while (numAccepted++ < aSocketConst.MAX_ACCEPTS_AT_ONCE) {
            // XXX: must check this.
            Socket sock = lss.accept();

            if (sock == null)
                break;

            LOGGER.debug("ListenThread: did accept on {}:{}", sock.getInetAddress().getHostAddress(), sock.getPort());
            
            ATcpConnection conn = new ATcpConnection(lss.servsock, sock.getInetAddress(), sock.getPort());
            
            LOGGER.debug("ListenThread: Created new conn {}", conn);
            
            SockState ss = SocketMgr.getFactory().newSockState(conn, sock, lss.writeClogThreshold);
            conn.sockState = ss;
            
            LOGGER.debug("ListenThread: Created new sockstate {}, and complete ListenSocketState ", ss);

            lss.complete(conn);
        }

        LOGGER.debug("ListenEventHandler: processAccept finished");
    }

    private void processListenRequest(aSocketRequest req) throws IOException {
        if (req instanceof ATcpListenRequest) {
            // This registers itself
            SocketMgr.getFactory().newListenSockState((ATcpListenRequest) req, selsource);
        } else if (req instanceof ATcpSuspendAcceptRequest) {
            ATcpSuspendAcceptRequest susreq = (ATcpSuspendAcceptRequest) req;

            ListenSockState lss = susreq.servsock.lss;
            if (lss == null) {
                throw new Error("ListenEventHandler: Got ATcpSuspendAcceptRequest for server socket "
                                + susreq.servsock + " with null lss!");
            }
            lss.suspend();
        } else if (req instanceof ATcpResumeAcceptRequest) {
            ATcpResumeAcceptRequest resreq = (ATcpResumeAcceptRequest) req;

            ListenSockState lss = resreq.servsock.lss;
            if (lss == null) {
                throw new Error("ListenEventHandler: Got ATcpResumeAcceptRequest for server socket "
                                + resreq.servsock + " with null lss!");
            }
            lss.resume();
        } else if (req instanceof ATcpCloseServerRequest) {
            ATcpCloseServerRequest creq = (ATcpCloseServerRequest) req;

            ListenSockState lss = creq.servsock.lss;
            // OK for lss to be null if closed down already
            if (lss != null)
                lss.close();
        } else {
            throw new IllegalArgumentException("Bad request type to enqueueListen");
        }
    }

    public void handleEvent(QueueElementIF qel) {
        LOGGER.debug("ListenEventHandler: Got QEL: {}", qel);

        try {
            if (qel instanceof SelectQueueElement) {
                ListenSockState lss = (ListenSockState) ((SelectQueueElement) qel).getAttachment();
                ((SelectQueueElement) qel).clearEvents();

                processAccept(lss);
            } else if (qel instanceof aSocketRequest) {
                processListenRequest((aSocketRequest) qel);
            } else {
                throw new IllegalArgumentException("ReadEventHandler: Got unknown event type " + qel);
            }
        } catch (Exception e) {
            System.err.println("ListenEventHandler: Got exception: " + e);
            e.printStackTrace();
        }
    }

    public void handleEvents(QueueElementIF qelarr[]) {
        for (int i = 0; i < qelarr.length; i++) {
            handleEvent(qelarr[i]);
        }
    }
}
