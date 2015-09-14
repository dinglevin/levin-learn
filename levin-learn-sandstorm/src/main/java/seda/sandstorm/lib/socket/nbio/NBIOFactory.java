/* 
 * Copyright (c) 2002 by Matt Welsh and The Regents of the University of 
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

package seda.sandstorm.lib.socket.nbio;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

import seda.nbio.SelectItem;
import seda.sandstorm.lib.socket.ATcpConnectRequest;
import seda.sandstorm.lib.socket.ATcpConnection;
import seda.sandstorm.lib.socket.ATcpListenRequest;
import seda.sandstorm.lib.socket.AUdpSocket;
import seda.sandstorm.lib.socket.SelectSourceIF;
import seda.sandstorm.lib.socket.SocketImplFactory;

/**
 * The NBIO implementation of aSocketImplFactory.
 * 
 * @author Matt Welsh
 */
public class NBIOFactory extends SocketImplFactory {
    protected SelectSourceIF newSelectSource() {
        return new SelectSource();
    }

    protected seda.sandstorm.lib.socket.SelectQueueElement newSelectQueueElement(
            Object item) {
        return new seda.sandstorm.lib.socket.nbio.SelectQueueElement(
                (SelectItem) item);
    }

    protected seda.sandstorm.lib.socket.SockState newSockState(
            ATcpConnection conn, Socket nbsock, int writeClogThreshold)
                    throws IOException {
        return new seda.sandstorm.lib.socket.nbio.SockState(conn, nbsock,
                writeClogThreshold);
    }

    protected seda.sandstorm.lib.socket.ConnectSockState newConnectSockState(
            ATcpConnectRequest req, SelectSourceIF selsource)
                    throws IOException {
        return new seda.sandstorm.lib.socket.nbio.ConnectSockState(req,
                selsource);
    }

    protected seda.sandstorm.lib.socket.ListenSockState newListenSockState(
            ATcpListenRequest req, SelectSourceIF selsource)
                    throws IOException {
        return new seda.sandstorm.lib.socket.nbio.ListenSockState(req,
                selsource);
    }

    protected seda.sandstorm.lib.socket.DatagramSockState newDatagramSockState(
            AUdpSocket sock, InetAddress addr, int port) throws IOException {
        return new seda.sandstorm.lib.socket.nbio.DatagramSockState(sock, addr,
                port);
    }

}
