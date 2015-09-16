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
    private EnqueuePredicate predicate;
    
    private Deque<EventElement> queue;
    private int queueSize;
    private Object blocker;
    private Map<Object, EventElement[]> provisionalTbl;

    /**
     * Create a EventQueueImpl with the given enqueue predicate.
     */
    public EventQueueImpl(String name, EnqueuePredicate predicate) {
        checkArgument(isNotBlank(name), "name is blank");
        
        this.name = name;
        this.predicate = predicate;
        this.LOGGER = LoggerFactory.getLogger(EventQueueImpl.class + "." + name);
        
        this.queue = Lists.newLinkedList();
        this.queueSize = 0;
        this.blocker = new Object();
        this.provisionalTbl = Maps.newHashMap();
    }

    /**
     * Create a EventQueueImpl with no enqueue and the given name. Used for
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
                if ((predicate != null) && (!predicate.accept(event))) {
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
            enqueue(event);
            return true;
        } catch (SinkFullException e) {
            return false;
        }
    }

    public void enqueueMany(EventElement[] events) throws SinkFullException {
        synchronized (blocker) {
            synchronized (queue) {
                if (predicate != null) {
                    for (EventElement event : events) {
                        if (!predicate.accept(event)) {
                            throw new SinkFullException("EventQueue is full!");
                        }
                    }
                }
                queueSize += events.length;
                for (EventElement event : events) {
                    queue.offerLast(event);
                }
            }
            blocker.notifyAll(); // wake up all sleepers
        }
    }

    public EventElement dequeue() {
        synchronized (blocker) {
            synchronized (queue) {
                if (queue.size() == 0) {
                    return null;
                }
                EventElement event = queue.pollFirst();
                queueSize--;
                return event;
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

                EventElement[] events = new EventElement[qs];
                for (int i = 0; i < qs; i++)
                    events[i] = queue.pollFirst();
                queueSize -= qs;
                return events;
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

                EventElement[] events = new EventElement[qs];
                for (int i = 0; i < qs; i++)
                    events[i] = queue.pollFirst();
                queueSize -= qs;
                return events;
            }
        }
    }

    public EventElement[] blockingDequeueAll(int timeoutMillis) {
        long goalTime = System.currentTimeMillis() + timeoutMillis;
        while (true) {
            synchronized (blocker) {
                EventElement[] rets = dequeueAll();
                if ((rets != null) || (timeoutMillis == 0)) {
                    return rets;
                }

                blockerWait(timeoutMillis);
                
                rets = dequeueAll();
                if (rets != null) {
                    return rets;
                }

                if (timeoutMillis != -1) {
                    if (System.currentTimeMillis() >= goalTime) {
                        return null;
                    }
                }
            }
        }
    }

    public EventElement[] blockingDequeue(int timeoutMillis, int num, boolean mustReturnNum) {
        long goalTime = System.currentTimeMillis() + timeoutMillis;
        while (true) {
            synchronized (blocker) {
                EventElement[] rets = dequeue(num, mustReturnNum);
                if ((rets != null) || (timeoutMillis == 0)) {
                    return rets;
                }

                blockerWait(timeoutMillis);

                rets = dequeue(num, mustReturnNum);
                if (rets != null) {
                    return rets;
                }

                if (timeoutMillis != -1) {
                    if (System.currentTimeMillis() >= goalTime) {
                        // Timeout - take whatever we can get
                        return this.dequeue(num);
                    }
                }
            }
        }
    }

    public EventElement[] blockingDequeue(int timeoutMillis, int num) {
        return blockingDequeue(timeoutMillis, num, false);
    }

    public EventElement blockingDequeue(int timeoutMillis) {
        long goalTime = System.currentTimeMillis() + timeoutMillis;
        while (true) {
            synchronized (blocker) {
                EventElement rets = dequeue();
                if ((rets != null) || (timeoutMillis == 0)) {
                    return rets;
                }
                
                blockerWait(timeoutMillis);
                
                rets = dequeue();
                if (rets != null) {
                    return rets;
                }

                if (timeoutMillis != -1) {
                    if (System.currentTimeMillis() >= goalTime)
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
    public Object enqueuePrepare(EventElement events[]) throws SinkException {
        synchronized (blocker) {
            synchronized (queue) {
                if (predicate != null) {
                    for (EventElement event : events) {
                        if (!predicate.accept(event)) {
                            throw new SinkFullException("EventQueue is full!");
                        }
                    }
                }
                queueSize += events.length;
                Object key = new Object();
                provisionalTbl.put(key, events);
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
                EventElement[] events = provisionalTbl.remove(key);
                if (events == null)
                    throw new IllegalArgumentException("Unknown enqueue key " + key);
                for (EventElement event : events) {
                    queue.offerLast(event);
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
                EventElement[] events = provisionalTbl.remove(key);
                if (events == null)
                    throw new IllegalArgumentException("Unknown enqueue key " + key);
                queueSize -= events.length;
            }
        }
    }

    /**
     * Set the enqueue predicate for this sink.
     */
    public void setEnqueuePredicate(EnqueuePredicate pred) {
        this.predicate = pred;
    }

    /**
     * Return the enqueue predicate for this sink.
     */
    public EnqueuePredicate getEnqueuePredicate() {
        return predicate;
    }

    public String toString() {
        return "EventQueueImpl <" + name + ">";
    }
    
    private void blockerWait(int timeoutMillis) {
        if (timeoutMillis == -1) {
            try {
                blocker.wait();
            } catch (InterruptedException ie) {
            }
        } else {
            try {
                blocker.wait(timeoutMillis);
            } catch (InterruptedException ie) {
            }
        }
    }
}
