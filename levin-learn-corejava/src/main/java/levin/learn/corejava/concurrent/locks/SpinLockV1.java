package levin.learn.corejava.concurrent.locks;

import java.util.concurrent.atomic.AtomicInteger;

public class SpinLockV1 extends LockAdaptor {
    private final AtomicInteger state = new AtomicInteger(0);
    private volatile Thread owner; // 这里owner字段可能存在中间值，不可靠，因而其他线程不可以依赖这个字段的值
    
    public void lock() {
        while (!state.compareAndSet(0, 1)) { }
        owner = Thread.currentThread();
    }
    
    public void unlock() {
        Thread currentThread = Thread.currentThread();
        if (owner != currentThread || !state.compareAndSet(1, 0)) {
            throw new IllegalStateException("The lock is not owned by thread: " + currentThread);
        }
        owner = null;
    }
}
