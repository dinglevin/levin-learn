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

package seda.sandstorm.api;

/**
 * This exception is thrown if a SinkIF is full; that is, that no more entries
 * can be pushed into the SinkIF immediately. This can occur because the sink
 * has reached a length threshold, or some other condition is preventing the
 * sink from (temporarily) accepting new elements.
 *
 * <p>
 * As opposed to SinkCloggedEvent, which is generated when a sink becomes full
 * asynchronously, this exception is thrown immediately when attempting to
 * enqueue onto a full sink.
 *
 * @see SinkCloggedEvent
 * @author Matt Welsh
 */
public class SinkFullException extends SinkException {
    private static final long serialVersionUID = 1L;

    public SinkFullException() {
        super();
    }

    public SinkFullException(String s) {
        super(s);
    }
}
