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

package seda.sandstorm.lib.http;

import java.io.IOException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;

import seda.sandstorm.api.ConfigData;
import seda.sandstorm.api.EventElement;
import seda.sandstorm.api.EventHandler;
import seda.sandstorm.api.EventSink;
import seda.sandstorm.api.Manager;
import seda.sandstorm.api.Profiler;
import seda.sandstorm.api.SinkCloggedEvent;
import seda.sandstorm.api.SinkClosedEvent;
import seda.sandstorm.api.SinkDrainedEvent;
import seda.sandstorm.lib.socket.ATcpConnection;
import seda.sandstorm.lib.socket.ATcpInPacket;
import seda.sandstorm.lib.socket.ATcpListenSuccessEvent;
import seda.sandstorm.lib.socket.ATcpServerSocket;
import seda.sandstorm.lib.socket.aSocketErrorEvent;
import seda.sandstorm.main.SandstormConfig;

/**
 * An httpServer is a SandStorm stage which accepts incoming HTTP connections.
 * The server has a client sink associated with it, onto which httpConnection
 * and httpRequest events are pushed. When a connection is closed, a
 * SinkClosedEvent is pushed, with the sink pointer set to the httpConnection
 * that closed.
 *
 * @author Matt Welsh (mdw@cs.berkeley.edu)
 * @see HttpConnection
 * @see HttpRequest
 */
public class HttpServer implements EventHandler, HttpConst {
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpServer.class);
    
    // These are protected to allow subclasses to use them
    protected int listenPort;
    protected ATcpServerSocket servsock;
    protected Manager mgr;
    protected EventSink mySink, clientSink;

    // ATcpConnection -> httpConnection
    private Map<ATcpConnection, HttpConnection> connTable;

    private static int num_svrs = 0;

    /**
     * Create an HTTP server listening for incoming connections on the default
     * port of 8080.
     */
    public HttpServer(Manager mgr, EventSink clientSink) throws Exception {
        this(mgr, clientSink, DEFAULT_HTTP_PORT);
    }

    /**
     * Create an HTTP server listening for incoming connections on the given
     * listenPort.
     */
    public HttpServer(Manager mgr, EventSink clientSink, int listenPort) throws Exception {
        this.mgr = mgr;
        this.clientSink = clientSink;
        this.listenPort = listenPort;

        this.connTable = Maps.newHashMap();

        // Create the stage and register it
        String sname = "httpServer " + num_svrs + " <port " + listenPort + ">";
        // Disable the RT controller for this stage
        mgr.getConfig().putBoolean("stages." + sname + ".rtController.enable", false);
        mgr.createStage(sname, this, null);
        num_svrs++;
    }

    /**
     * The Sandstorm stage initialization method.
     */
    public void init(ConfigData config) throws Exception {
        mySink = config.getStage().getSink();

        servsock = new ATcpServerSocket(listenPort, mySink, WRITE_CLOG_THRESHOLD);
    }

    /**
     * The Sandstorm stage destroy method.
     */
    public void destroy() {
    }

    /**
     * The main event handler.
     */
    public void handleEvent(EventElement event) {
        LOGGER.debug("httpServer got event: {}", event);

        if (event instanceof ATcpInPacket) {
            ATcpInPacket pkt = (ATcpInPacket) event;

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("httpServer got packet: -----------------------");
                LOGGER.trace("{}\n----------------------------------", new String(pkt.getBytes()));
            }

            HttpConnection hc = connTable.get(pkt.getConnection());
            if (hc == null)
                return; // Connection may have been closed

            try {
                hc.parsePacket(pkt);
            } catch (IOException ioe) {
                LOGGER.error("Error on packet processing for connection " + hc, ioe);
                // XXX Should close connection
            }
        } else if (event instanceof ATcpConnection) {
            ATcpConnection conn = (ATcpConnection) event;
            HttpConnection hc = new HttpConnection(conn, this, clientSink);
            connTable.put(conn, hc);

            // Profile the connection if profiling enabled
            Profiler profiler = mgr.getProfiler();
            SandstormConfig cfg = mgr.getConfig();
            if ((profiler != null) && cfg.getBoolean("global.profile.sockets"))
                profiler.add(conn.toString(), conn);
            conn.startReader(mySink);
        } else if (event instanceof aSocketErrorEvent) {
            LOGGER.error("httpServer got error: {}", event);
        } else if (event instanceof SinkDrainedEvent) {
            // Ignore

        } else if (event instanceof SinkCloggedEvent) {
            // Some connection is clogged; tell the user
            SinkCloggedEvent sce = (SinkCloggedEvent) event;
            HttpConnection hc = (HttpConnection) connTable.get(sce.sink);
            if (hc != null)
                clientSink.enqueueLossy(new SinkCloggedEvent(hc, null));

        } else if (event instanceof SinkClosedEvent) {
            // Some connection closed; tell the user
            SinkClosedEvent sce = (SinkClosedEvent) event;
            HttpConnection hc = (HttpConnection) connTable.get(sce.sink);
            if (hc != null) {
                clientSink.enqueueLossy(new SinkClosedEvent(hc));
                cleanupConnection(hc);
            }

        } else if (event instanceof ATcpListenSuccessEvent) {
            clientSink.enqueueLossy(event);
        }
    }

    public void handleEvents(EventElement[] events) {
        for (int i = 0; i < events.length; i++) {
            handleEvent(events[i]);
        }
    }

    void cleanupConnection(HttpConnection hc) {
        connTable.remove(hc.getConnection());
    }

    public String toString() {
        return "httpServer [listen=" + listenPort + "]";
    }

    /**
     * Register a sink to receive incoming packets on this connection.
     */
    public void registerSink(EventSink sink) {
        this.clientSink = sink;
    }

    /**
     * Suspend acceptance of new connections on this server. This request will
     * not be effective immediately.
     */
    public void suspendAccept() {
        servsock.suspendAccept();
    }

    /**
     * Resume acceptance of new connections on this server. This request will
     * not be effective immediately.
     */
    public void resumeAccept() {
        servsock.resumeAccept();
    }

    // Return my sink so that httpConnection can redirect
    // packet completions to it
    EventSink getSink() {
        return mySink;
    }

    /**
     * Return the server socket being used by this httpServer.
     */
    public ATcpServerSocket getServerSocket() {
        return servsock;
    }
}
