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

package seda.sandstorm.timer;

import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import seda.sandstorm.api.EventElement;
import seda.sandstorm.api.EventSink;
import seda.sandstorm.api.Profilable;

/**
 * The Timer class provides a mechanism for registering timer events that will
 * go off at some future time. The future time can be specified in absolute or
 * relative terms. When the timer goes off, an element is placed on a queue.
 * There is no way to unregister a timer. Events will be delivered guaranteed,
 * but the time that they are delivered may slip depending on stuff like how
 * loaded the system is and all that.
 * <P>
 * WARNING: you should use cancelEvent to cancel timers that you no longer need,
 * otherwise you will waste many, many cycles on unneeded timer firings. This
 * was the bottleneck in vSpace and the DDS before we fixed it. For example, if
 * you set a timer to go off on a cross-CPU task to detect failure, then if the
 * task returns successfully, cancel the timer!
 *
 * @author Matt Welsh and Steve Gribble
 */

public class Timer implements Runnable, Profilable {
    private static final Logger LOGGER = LoggerFactory.getLogger(Timer.class);
    
    private final Thread thread;
    private final Lock lock;
    private final Condition condition;
    
    private volatile boolean threadDied;
    private int numEvents = 0;
    
    private TimerNode head = null;
    private TimerNode tail = null;

    public Timer() {
        lock = new ReentrantLock();
        condition = lock.newCondition();
        threadDied = false;
        thread = new Thread(this, "SandStorm Timer thread");
        thread.start();
    }

    /**
     * Object <code>obj</code> will be placed on SinkIF <code>queue</code> no
     * earlier than <code>millis</code> milliseconds from now.
     *
     * @param millis
     *            the number of milliseconds from now when the event will take place
     * @param obj
     *            the object that will be placed on the queue
     * @param queue
     *            the queue on which the object will be placed
     */
    public TimerHandle registerEvent(long delay, EventElement event, EventSink queue) {
        TimerNode node = new TimerNode(delay + System.currentTimeMillis(), event, queue);
        insertEvent(node);
        return node;
    }

    /**
     * Object <code>obj</code> will be placed on SinkIF <code>queue</code> no
     * earlier than absolute time <code>the_date</code>.
     *
     * @param happening
     *            the date when the event will take place - if this date is in
     *            the past, the event will happen right away
     * @param obj
     *            the object that will be placed on the queue
     * @param queue
     *            the queue on which the object will be placed
     */
    public TimerHandle registerEvent(Date happening, EventElement obj, EventSink queue) {
        TimerNode node = new TimerNode(happening.getTime(), obj, queue);
        insertEvent(node);
        return node;
    }

    /**
     * Kills off this timer object, dropping all pending events on floor.
     */
    public void doneWithTimer() {
        threadDied = true;
        lock.lock();
        try {
            condition.signalAll();
        } finally {
            lock.unlock();
        }
    }

    /**
     * How many events yet to fire?
     */
    public int size() {
        return numEvents;
    }

    /**
     * Return the profile size of this timer.
     */
    public int profileSize() {
        return size();
    }
    
    public boolean isDead() {
        return threadDied;
    }

    /**
     * Cancels all events.
     */
    public void cancelAll() {
        lock.lock();
        try {
            head = tail = null;
            numEvents = 0;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Cancels the firing of this timer event.
     *
     * @param handle
     *            the ssTimer.ssTimerEvent to cancel. This ssTimerEvent is
     *            returned to you when you call registerEvent
     */
    public void cancelEvent(TimerHandle handle) {
        Preconditions.checkNotNull(handle, "handle is null");
        if (!handle.isActive()) {
            return;
        }
        
        TimerNode node = (TimerNode) handle;
        lock.lock();
        try {
            if (handle == tail && handle == head) {
                tail = head = null;
                numEvents--;
                return;
            }
            
            if (handle == tail) {
                tail = tail.getPrev();
                tail.setNext(null);
                numEvents--;
                return;
            }
            
            if (handle == head) {
                head = head.getNext();
                head.setPrev(null);
                numEvents--;
                return;
            } 

            if ((node.getNext() != null) && (node.getPrev() != null)) {
                node.getPrev().setNext(node.getNext());
                numEvents--;
                return;
            }
        } finally {
            node.unlink();
            lock.unlock();
        }
    }

    // takes the event, does insertion-sort into ssTimerEvent linked list
    private void insertEvent(TimerNode node) {
        lock.lock();
        try {
            if (head == null) {
                tail = node;
                head = node;
                condition.signal();
            } else if (node.getHappening() < head.getHappening()) {
                node.setNext(head);
                head.setPrev(node);
                head = node;
                condition.signal();
            } else if (node.getHappening() >= tail.getHappening()) {
                node.setPrev(tail);
                tail.setNext(node);
                tail = node;
            } else {
                TimerNode cur = tail;
                TimerNode prev = tail.getPrev();
                while (prev != null) {
                    if (node.getHappening() >= prev.getHappening()) {
                        break;
                    }
                    cur = prev;
                    prev = prev.getPrev();
                }
                cur.setPrev(node);
                prev.setNext(node);
            }
            numEvents++;
        } finally {
            lock.unlock();
        }
    }

    private void processHead() {
        long curTime = System.currentTimeMillis();
        if (head.getHappening() <= curTime) {
            // fire off event
            TimerNode fire = head;
            head = head.getNext();
            if (head == null) {
                tail = null;
            } else {
                head.setPrev(null);
            }

            if ((head == null) && (numEvents != 1)) {
                LOGGER.warn("No more events to process, but still have {} pending. BUG", (numEvents - 1));
            }

            fire.unlink();
            fire.eventReady();
            numEvents--;
        } else {
            // sleep till head
            long waitTime = head.getHappening() - curTime;
            if (waitTime > 0) {
                try {
                    condition.await(waitTime, TimeUnit.MILLISECONDS);
                } catch (InterruptedException ie) {
                }
            }
        }
    }

    public void run() {
        lock.lock();
        try {
            while (!threadDied) {
                try {
                    if (head != null) {
                        processHead();
                    } else {
                        if (threadDied)
                            return;

                        try {
                            condition.await(500, TimeUnit.MILLISECONDS);
                        } catch (InterruptedException ie) {
                        }
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        } finally {
            lock.unlock();
        }
    }
}
