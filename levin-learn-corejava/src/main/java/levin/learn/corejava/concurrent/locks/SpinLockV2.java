package levin.learn.corejava.concurrent.locks;

import java.util.concurrent.atomic.AtomicReference;

public class SpinLockV2 {
    private final AtomicReference<Thread> owner = new AtomicReference<Thread>(null);
    
    public void lock() {
        final Thread currentThread = Thread.currentThread();
        while (!owner.compareAndSet(null, currentThread)) { }
    }
    
    public void unlock() {
        Thread currentThread = Thread.currentThread();
        if (!owner.compareAndSet(currentThread, null)) {
            throw new IllegalStateException("The lock is not owned by thread: " + currentThread);
        }
    }
}
