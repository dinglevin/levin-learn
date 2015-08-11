package levin.learn.corejava.concurrent.locks;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

public abstract class LockAdaptor implements Lock {

    @Override
    public void lockInterruptibly() throws InterruptedException {
        lock();
    }

    @Override
    public boolean tryLock() {
        throw new UnsupportedOperationException("tryLock");
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit)
            throws InterruptedException {
        throw new UnsupportedOperationException("tryLock");
    }

    @Override
    public Condition newCondition() {
        throw new UnsupportedOperationException("newCondition");
    }
}
