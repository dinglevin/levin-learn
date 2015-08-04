package levin.learn.corejava.concurrent.locks;

public class Lock {
    private static class Sync extends AbstractQueuedSynchronizer {
        
    }

    private Sync sync = new Sync();
    
    public void lock() {
        sync.acquire(1);
    }
    
    public void unlock() {
        sync.release(1);
    }
}
