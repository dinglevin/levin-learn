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

import seda.sandstorm.api.*;
import seda.sandstorm.core.*;
import seda.sandstorm.lib.socket.*;

import java.util.*;
import java.io.*;
import java.net.*;

/**
 * This class represents a single HTTP connection. When an httpServer
 * receives a connection, an httpConnection is pushed to the user.
 * To send HTTP responses to a client, you can enqueue an httpResponse
 * object on the corresponding httpConnection.
 *
 * @author Matt Welsh
 * @see HttpRequest
 * @see HttpResponse
 */
public class HttpConnection extends SimpleSink implements HttpConst, EventElement {

  private static final boolean DEBUG = false;

  private ATcpConnection tcpconn;
  private HttpServer hs;
  private EventSink compQ;
  private HttpPacketReader hpr;

  /** 
   * Can be used by applications to associate an arbitrary data object 
   * with this connection.
   */
  public Object userTag;

  /**
   * Package-internal: Create an httpConnection with the given TCP 
   * connection and completion queue.
   */
  HttpConnection(ATcpConnection tcpconn, HttpServer hs, EventSink compQ) {
    this.tcpconn = tcpconn;
    this.hs = hs;
    this.compQ = compQ;
    this.hpr = new HttpPacketReader(this, compQ);

    // Push myself to user
    compQ.enqueueLossy(this);
  }

  /**
   * Package-internal: Parse the data contained in the given TCP packet.
   */
  void parsePacket(ATcpInPacket pkt) throws IOException {
    hpr.parsePacket(pkt);
  }

  /**
   * Return the ATcpConnection associated with this connection.
   */
  public ATcpConnection getConnection() {
    return tcpconn;
  }

  public String toString() {
    return "httpConnection [conn="+tcpconn+"]";
  }

  /* SinkIF methods ***********************************************/

  /**
   * Enqueue outgoing data on this connection. The 'element' must be
   * of type httpResponder.
   */
  public void enqueue(EventElement element) throws SinkException {
    if (DEBUG) System.err.println("httpConnection.enqueue: "+element);
    HttpResponder resp = (HttpResponder)element;
    HttpResponse packet = resp.getResponse();
    BufferEvent bufarr[] = packet.getBuffers(resp.sendHeader());
    tcpconn.enqueueMany(bufarr);
  }

  /**
   * Enqueue outgoing data on this connection. The 'element' must be
   * of type httpResponder.
   */
  public boolean enqueueLossy(EventElement element) {
    if (DEBUG) System.err.println("httpConnection.enqueue_lossy: "+element);
    HttpResponder resp = (HttpResponder)element;
    HttpResponse packet = resp.getResponse();
    BufferEvent bufarr[] = packet.getBuffers(resp.sendHeader());
    try {
      tcpconn.enqueueMany(bufarr);
    } catch (SinkException se) {
      return false;
    }
    return true;
  }

  /**
   * Enqueue outgoing data on this connection. Each item in the 
   * elements array must be of type httpResponse.
   */
  public void enqueueMany(EventElement elements[]) throws SinkException {
    for (int i = 0; i < elements.length; i++) {
      enqueue(elements[i]);
    }
  }

  /**
   * Return the number of outgoing packets waiting to be sent.
   */
  public int size() {
    return tcpconn.size();
  }

  /**
   * Close the connection.
   */
  public void close(final EventSink compQ) throws SinkClosedException {
    // XXX For now, allow a connection to be closed multiple times.
    // Tricky bit below: Provide anonymous SinkIF as 'compQ' which
    // we re-enqueue onto user compQ as appropriate SinkDrainedEvent!

    hs.cleanupConnection(this);
    tcpconn.close(new SimpleSink() {
	public void enqueue(EventElement qel) throws SinkException {
	  compQ.enqueue(new SinkClosedEvent(HttpConnection.this));
	}
      });
  }

  /**
   * Flush the connection; a SinkFlushedEvent will be pushed to the
   * user when all packets have drained.
   */
  public void flush(EventSink compQ) throws SinkClosedException {
    tcpconn.flush(compQ);
  }

  public Object enqueuePrepare(EventElement enqueueMe[]) throws SinkException {
    return tcpconn.enqueuePrepare(enqueueMe);
  }

  public void enqueueCommit(Object key) {
    tcpconn.enqueueCommit(key);
  }

  public void enqueueAbort(Object key) {
    tcpconn.enqueueAbort(key);
  }


}
