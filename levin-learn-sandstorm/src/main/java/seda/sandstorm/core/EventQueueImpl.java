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

package seda.sandstorm.core;

import static com.google.common.base.Preconditions.checkArgument;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.Deque;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import seda.sandstorm.api.EnqueuePredicate;
import seda.sandstorm.api.EventElement;
import seda.sandstorm.api.EventQueue;
import seda.sandstorm.api.Profilable;
import seda.sandstorm.api.SinkException;
import seda.sandstorm.api.SinkFullException;

public class EventQueueImpl implements EventQueue, Profilable {
    private final Logger LOGGER;
    
    private final String name;
    private EnqueuePredicate pred;
    
    private Deque<EventElement> queue;
    private int queueSize;
    private Object blocker;
    private Map<Object, EventElement[]> provisionalTbl;

    /**
     * Create a EventQueueImpl with the given enqueue predicate.
     */
    public EventQueueImpl(String name, EnqueuePredicate pred) {
        checkArgument(isNotBlank(name), "name is blank");
        
        this.name = name;
        this.pred = pred;
        this.LOGGER = LoggerFactory.getLogger(EventQueueImpl.class + "." + name);
        
        this.queue = Lists.newLinkedList();
        this.queueSize = 0;
        this.blocker = new Object();
        this.provisionalTbl = Maps.newHashMap();
    }

    /**
     * Create a FiniteQueue with no enqueue and the given name. Used for
     * debugging.
     */
    public EventQueueImpl(String name) {
        this(name, null);
    }

    /**
     * Return the size of the queue.
     */
    public int size() {
        synchronized (blocker) {
            synchronized (queue) {
                return queueSize;
            }
        }
    }

    public void enqueue(EventElement event) throws SinkFullException {
        LOGGER.trace("enqueue event {}", event);
        
        synchronized (blocker) {
            synchronized (queue) {
                if ((pred != null) && (!pred.accept(event))) {
                    throw new SinkFullException("EventQueue is full!");
                }
                
                queueSize++;
                queue.offerLast(event); // wake up one blocker
            }
            
            blocker.notifyAll();
        }
        
        LOGGER.trace("enqueued event {}", event);
    }

    public boolean enqueueLossy(EventElement event) {
        try {
            this.enqueue(event);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public void enqueueMany(EventElement[] event) throws SinkFullException {
        synchronized (blocker) {
            int qlen = event.length;

            synchronized (queue) {
                if (pred != null) {
                    int i = 0;
                    while ((i < qlen) && (pred.accept(event[i])))
                        i++;
                    if (i != qlen)
                        throw new SinkFullException("FiniteQueue is full!");
                }

                queueSize += qlen;
                for (int i = 0; i < qlen; i++) {
                    queue.offerLast(event[i]);
                }
            }
            blocker.notifyAll(); // wake up all sleepers
        }
    }

    public EventElement dequeue() {
        EventElement el = null;
        synchronized (blocker) {
            synchronized (queue) {
                if (queue.size() == 0)
                    return null;

                el = queue.pollFirst();
                queueSize--;
                return el;
            }
        }
    }

    public EventElement[] dequeueAll() {
        synchronized (blocker) {
            synchronized (queue) {
                int qs = queue.size();
                if (qs == 0)
                    return null;

                EventElement[] retIF = new EventElement[qs];
                for (int i = 0; i < qs; i++)
                    retIF[i] = queue.pollFirst();
                queueSize -= qs;
                return retIF;
            }
        }
    }

    public EventElement[] dequeue(int num) {
        synchronized (blocker) {
            synchronized (queue) {
                int qs = Math.min(queue.size(), num);

                if (qs == 0)
                    return null;

                EventElement[] retIF = new EventElement[qs];
                for (int i = 0; i < qs; i++)
                    retIF[i] = queue.pollFirst();
                queueSize -= qs;
                return retIF;
            }
        }
    }

    public EventElement[] dequeue(int num, boolean mustReturnNum) {
        synchronized (blocker) {
            synchronized (queue) {
                int qs;

                if (!mustReturnNum) {
                    qs = Math.min(queue.size(), num);
                } else {
                    if (queue.size() < num)
                        return null;
                    qs = num;
                }

                if (qs == 0)
                    return null;

                EventElement[] retIF = new EventElement[qs];
                for (int i = 0; i < qs; i++)
                    retIF[i] = queue.pollFirst();
                queueSize -= qs;
                return retIF;
            }
        }
    }

    public EventElement[] blockingDequeueAll(int timeout_millis) {
        EventElement[] rets = null;
        long goal_time;
        int num_spins = 0;

        LOGGER.debug("**** B_DEQUEUE_A ({}) **** Entered", name);

        goal_time = System.currentTimeMillis() + timeout_millis;
        while (true) {
            synchronized (blocker) {
                LOGGER.debug("**** B_DEQUEUE_A ({}) **** Doing D_A", name);
                
                rets = this.dequeueAll();
                LOGGER.debug("**** B_DEQUEUE_A ({}) **** RETS IS {}", name, rets);
                
                if ((rets != null) || (timeout_millis == 0)) {
                    LOGGER.debug("**** B_DEQUEUE_A ({}) **** RETURNING (1)", name);
                    return rets;
                }

                if (timeout_millis == -1) {
                    try {
                        blocker.wait();
                    } catch (InterruptedException ie) {
                    }
                } else {
                    try {
                        LOGGER.debug("**** B_DEQUEUE_A ({}) **** WAITING ON BLOCKER", name);
                        blocker.wait(timeout_millis);
                    } catch (InterruptedException ie) {
                    }
                }

                LOGGER.debug("**** B_DEQUEUE_A ({}) **** Doing D_A (2)", name);
                
                rets = this.dequeueAll();
                LOGGER.debug("**** B_DEQUEUE_A ({}) **** RETS(2) IS {}", name, rets);
                if (rets != null) {
                    LOGGER.debug("**** B_DEQUEUE_A ({}) **** RETURNING(2)", name);
                    return rets;
                }

                if (timeout_millis != -1) {
                    if (System.currentTimeMillis() >= goal_time) {
                        LOGGER.debug("**** B_DEQUEUE_A ({}) **** RETURNING(3)", name);
                        return null;
                    }
                }
            }
        }
    }

    public EventElement[] blockingDequeue(int timeout_millis, int num, boolean mustReturnNum) {
        EventElement[] rets = null;
        long goal_time;
        int num_spins = 0;

        goal_time = System.currentTimeMillis() + timeout_millis;
        while (true) {
            synchronized (blocker) {

                rets = this.dequeue(num, mustReturnNum);
                if ((rets != null) || (timeout_millis == 0)) {
                    return rets;
                }

                if (timeout_millis == -1) {
                    try {
                        blocker.wait();
                    } catch (InterruptedException ie) {
                    }
                } else {
                    try {
                        blocker.wait(timeout_millis);
                    } catch (InterruptedException ie) {
                    }
                }

                rets = this.dequeue(num, mustReturnNum);
                if (rets != null) {
                    return rets;
                }

                if (timeout_millis != -1) {
                    if (System.currentTimeMillis() >= goal_time) {
                        // Timeout - take whatever we can get
                        return this.dequeue(num);
                    }
                }
            }
        }
    }

    public EventElement[] blockingDequeue(int timeout_millis, int num) {
        return blockingDequeue(timeout_millis, num, false);
    }

    public EventElement blockingDequeue(int timeout_millis) {
        EventElement rets = null;
        long goal_time;
        int num_spins = 0;

        goal_time = System.currentTimeMillis() + timeout_millis;
        while (true) {
            synchronized (blocker) {

                rets = this.dequeue();
                if ((rets != null) || (timeout_millis == 0)) {
                    return rets;
                }

                if (timeout_millis == -1) {
                    try {
                        blocker.wait();
                    } catch (InterruptedException ie) {
                    }
                } else {
                    try {
                        blocker.wait(timeout_millis);
                    } catch (InterruptedException ie) {
                    }
                }

                rets = this.dequeue();
                if (rets != null) {
                    return rets;
                }

                if (timeout_millis != -1) {
                    if (System.currentTimeMillis() >= goal_time)
                        return null;
                }
            }
        }
    }

    /**
     * Return the profile size of the queue.
     */
    public int profileSize() {
        return size();
    }

    /**
     * Provisionally enqueue the given elements.
     */
    public Object enqueuePrepare(EventElement enqueueMe[])
            throws SinkException {
        int qlen = enqueueMe.length;
        synchronized (blocker) {
            synchronized (queue) {
                if (pred != null) {
                    int i = 0;
                    while ((i < qlen) && (pred.accept(enqueueMe[i])))
                        i++;
                    if (i != qlen)
                        throw new SinkFullException("FiniteQueue is full!");
                }
                queueSize += qlen;
                Object key = new Object();
                provisionalTbl.put(key, enqueueMe);
                return key;
            }
        }
    }

    /**
     * Commit a provisional enqueue.
     */
    public void enqueueCommit(Object key) {
        synchronized (blocker) {
            synchronized (queue) {
                EventElement elements[] = (EventElement[]) provisionalTbl
                        .remove(key);
                if (elements == null)
                    throw new IllegalArgumentException(
                            "Unknown enqueue key " + key);
                for (int i = 0; i < elements.length; i++) {
                    queue.offerLast(elements[i]);
                }
            }
            blocker.notifyAll();
        }
    }

    /**
     * Abort a provisional enqueue.
     */
    public void enqueueAbort(Object key) {
        synchronized (blocker) {
            synchronized (queue) {
                EventElement elements[] = (EventElement[]) provisionalTbl
                        .remove(key);
                if (elements == null)
                    throw new IllegalArgumentException(
                            "Unknown enqueue key " + key);
                queueSize -= elements.length;
            }
        }
    }

    /**
     * Set the enqueue predicate for this sink.
     */
    public void setEnqueuePredicate(EnqueuePredicate pred) {
        this.pred = pred;
    }

    /**
     * Return the enqueue predicate for this sink.
     */
    public EnqueuePredicate getEnqueuePredicate() {
        return pred;
    }

    public String toString() {
        return "EventQueueImpl <" + name + ">";
    }
}
