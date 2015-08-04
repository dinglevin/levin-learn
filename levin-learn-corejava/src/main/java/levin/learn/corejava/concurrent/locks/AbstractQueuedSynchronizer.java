package levin.learn.corejava.concurrent.locks;

public class AbstractQueuedSynchronizer extends AbstractOwnableSynchronizer {
    protected AbstractQueuedSynchronizer() { }
    
    private volatile int state;
    private volatile Node head;
    private volatile Node tail;
    
    protected static class Node {
        volatile Thread owner;
        volatile Node prev;
        volatile Node next;
    }
    
    public void acquire(int state) {
        
    }
    
    public void release(int state) {
        
    }
}
