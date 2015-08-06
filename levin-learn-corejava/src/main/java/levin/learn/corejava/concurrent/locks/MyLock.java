package levin.learn.corejava.concurrent.locks;

import java.lang.reflect.Field;
import java.util.concurrent.locks.LockSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sun.misc.Unsafe;

@SuppressWarnings("restriction")
public class MyLock {
    private static final boolean LOG_ON = true;
    
    private static final Logger logger = LoggerFactory.getLogger(MyLock.class);
    
    private volatile Thread current;
    private volatile Node head;
    private volatile Node tail;
    
    public MyLock() { 
        // Keep it simple, head is just a place holder
        tail = head = new Node(null);
        current = null;
    }
    
    public void lock() {
        final Node curNode = head.next;
        if (curNode != null && curNode.owner == Thread.currentThread()) {
            throw new IllegalStateException("This is not a reentrant lock");
        }
        
        // Put the latest node to a queue first, then check if the it is the first node
        // this way, the list is the only shared resource to deal with
        Node node = new Node();
        if (enqueue(node)) {
            current = node.owner;
            node.state = Node.RUNNING;
            if (LOG_ON) {
                logger.info("Node[{}] Got lock", node);
            }
        } else {
            node.state = Node.PARKED;
            
            if (LOG_ON) {
                logger.info("Parked before: {}", node.prev);
            }
            
            LockSupport.park(this); // This may return "spuriously"!!
            while (node.prev != head) {
                //throw new IllegalStateException("Running node is not the first node, prev=" + node.prev);
                if (LOG_ON) {
                    logger.error("=====================Running node is not the first node, prev={}, interrupt={}", node.prev, Thread.interrupted());
                }
                LockSupport.park(this);
            }
            
            node.state = Node.RUNNING;
            current = node.owner;
            
            if (LOG_ON) {
                logger.info("Wake up, next[{}]", node.next);
            }
        }
    }
    
    public void unlock() {
        Node curNode = unlockValidate();
        curNode.state = Node.FINISH;
        
        Node next = curNode.next;
        if (next != null) {
            if (!compareAndSetNext(head, curNode, next)) {
                throw new IllegalStateException("Expect: " + curNode + ", was: " + 
                        head.next + ", next: " + next);
            }
            next.prev = head;
            if (next.owner == null) {
                throw new IllegalStateException("owner is null: " + next);
            }
            
            if (LOG_ON) {
                logger.info("Unparking node: {}", next);
            }
            LockSupport.unpark(next.owner);
        } else {
            if (!compareAndSetTail(curNode, head)) {
                // Another node queued during the time, so we have to unlock that, or else, this node can never unparked
                if (LOG_ON) {
                    logger.info("Another node queued: {}", tail);
                }
                unlock();
            } else {
                if (compareAndSetNext(head, curNode, null)) {
                    if (LOG_ON) {
                        logger.info("Clear current queue");
                    }
                } else {
                    // This could happen because when clear tail, another thread may have queued and change the head.next
                    // but we don't need to unlock in this case as we already correctly set the tail
                    if (LOG_ON) {
                        logger.info("head.next changed: {}, tail: {}, cur: {}, next: {}", head.next, tail, curNode, next);
                    }
                }
            }
        }
    }
    
    protected boolean enqueue(Node node) {
        while (true) {
            final Node preTail = tail;
            node.prev = preTail;
            if (compareAndSetTail(preTail, node)) {
                preTail.next = node;
                return node.prev == head;
            }
        }
    }
    
    private Node unlockValidate() {
        final Thread owner = current;
        final Thread cur = Thread.currentThread();
        final Node curNode = head.next;
        
        if (owner == null) {
            throw new IllegalStateException("No owner thread attached to this Lock");
        }
        if (owner != cur) {
            throw new IllegalStateException("Unlock thread is not the same as lock thread[" + owner + ", " + 
                    cur + "], curNode: " + curNode);
        }
        
        if (curNode == null) {
            throw new IllegalStateException("No node exists in the waiting list");
        }
        if (curNode.state != Node.FINISH && curNode.state != Node.RUNNING) {
            throw new IllegalStateException("Not not in running or finished state: " + curNode);
        }
        
        return curNode;
    }
    
    protected static class Node {
        static final int INIT = 0;
        static final int RUNNING = 1;
        static final int PARKED = 2;
        static final int FINISH = 3;
        
        volatile Thread owner;
        volatile Node prev;
        volatile Node next;
        volatile int state;
        
        public Node(Thread owner) {
            this.owner = owner;
            this.state = INIT;
        }
        
        public Node() {
            this(Thread.currentThread());
        }
        
        public String toString() {
            return "{" + owner + ", state: " + state + "}";
        }
    }
    
    private boolean compareAndSetTail(Node expected, Node actual) {
        return UNSAFE.compareAndSwapObject(this, tailOffset, expected, actual);
    }
    
    private static boolean compareAndSetNext(Node instance, Node expected, Node actual) {
        return UNSAFE.compareAndSwapObject(instance, nextOffset, expected, actual);
    }
    
    // Hotspot implementation via intrinsics API
    private static final Unsafe UNSAFE;
    private static final long tailOffset;
    private static final long nextOffset;
    static {
        try {
            UNSAFE = getUnsafe();
            Class<?> tk = MyLock.class;
            tailOffset = UNSAFE.objectFieldOffset(tk.getDeclaredField("tail"));
            nextOffset = UNSAFE.objectFieldOffset(Node.class.getDeclaredField("next"));
        } catch (Exception ex) { 
            throw new Error(ex); 
        }
    }
    
    public static Unsafe getUnsafe() {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            return (Unsafe)f.get(null);
        } catch (Exception e) { 
            throw new Error("Not able to get Unsafe instance from theUnsafe field of Unsafe class");
        }
     }
}