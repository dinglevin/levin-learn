package levin.learn.corejava.concurrent.locks;

import org.junit.Test;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

@RunWith(Theories.class)
public class MyLockTest {
    @Test
    public void testSingleThreadLock() {
        MyLock lock = new MyLock();
        lock.lock();
        try {
            System.out.println("Inside the lock");
        } finally {
            lock.unlock();
        }
        System.out.println("Outside the lock");
    }
    
    @Test
    public void testTwoSequenceLock() {
        MyLock lock = new MyLock();
        lock.lock();
        try {
            System.out.println("Inside the first lock");
        } finally {
            lock.unlock();
        }
        
        lock.lock();
        try {
            System.out.println("Inside the second lock");
        } finally {
            lock.unlock();
        }
        System.out.println("Outside the lock");
    }
    
    @Test
    public void testTwoConcurrentLock() throws InterruptedException {
        final MyLock lock = new MyLock();
        
        Thread[] threads = new Thread[2];
        threads[0] = new Thread(new Runnable() {
            public void run() {
                lock.lock();
                try {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    
                    System.out.println("Inside the first lock");
                } finally {
                    lock.unlock();
                }
            }
        });
        
        threads[1] = new Thread(new Runnable() {
            public void run() {
                System.out.println("Start the second one");
                lock.lock();
                try {
                    System.out.println("Inside the second lock");
                } finally {
                    lock.unlock();
                }
            }
        });
        
        threads[0].start();
        threads[1].start();
        
        threads[0].join();
        threads[1].join();
        
        System.out.println("Outside the lock");
    }
    
    @Test(expected = IllegalStateException.class)
    public void testNonReentrantLock() {
        MyLock lock = new MyLock();
        lock.lock();
        try {
            System.out.println("Inside first lock block");
            lock.lock();
            try {
                System.out.println("Inside second lock block");
            } finally {
                lock.unlock();
            }
        } finally {
            lock.unlock();
        }
    }
    
    @Test
    public void testLockInMultiThreadSequence() throws Exception {
        final int COUNT = 100;
        Thread[] threads = new Thread[COUNT];
        MyLock lock = new MyLock();
        Runner runner = new Runner(lock, COUNT);
        
        for (int i = 0; i < COUNT; i++) {
            threads[i] = new Thread(runner, "thread" + i);
            threads[i].start();
        }
        
        for (int i = 0; i < COUNT; i++) {
            threads[i].join();
        }
        
        runner.verify();
    }
    
    public static @DataPoints int[][] candidates = {{100, 50, 10}, {100, 20, 5}, {100, 30, 1}, {1000, 200, 10}};
    
    @Theory
    public void testLockInMultiThread(final int[] params) throws Exception {
        for (int i = 0; i < 100; i++) {
            doTestLockInMultiThread(params[0], params[1], params[2]);
        }
    }
    
    private void doTestLockInMultiThread(final int count, final int mid, final int sleep) throws Exception {
        Thread[] threads = new Thread[count];
        MyLock lock = new MyLock();
        Runner runner = new Runner(lock, count);
        
        for (int i = 0; i < mid; i++) {
            threads[i] = new Thread(runner, "thread-" + i);
            threads[i].start();
        }
        System.out.println("Sleeping");
        Thread.sleep(sleep);
        System.out.println("Sleep finish");
        for (int i = mid; i < count; i++) {
            threads[i] = new Thread(runner, "thread-" + i);
            threads[i].start();
        }
        
        for (int i = 0; i < count; i++) {
            threads[i].join();
        }
        
        runner.verify();
    }
    
    private static class Runner implements Runnable {
        private final MyLock lock;
        
        private volatile int state = 0;
        private int[] stateResults;
        
        public Runner(MyLock lock, int count) {
            this.lock = lock;
            this.stateResults = new int[count];
        }
        
        public void run() {
            //System.out.println(Thread.currentThread() + ": locking");
            lock.lock();
            try {
                stateResults[state++] += 1;
                //System.out.println(Thread.currentThread() + ": " + state);
            } finally {
                lock.unlock();
                //System.out.println(Thread.currentThread() + ": unlock");
            }
        }
        
        public void verify() {
            boolean pass = true;
            for (int i = 0; i < stateResults.length; i++) {
                if (stateResults[i] != 1) {
                    System.out.println("state[" + i + "]=" + stateResults[i]);
                    pass = false;
                }
            }
            if (!pass) {
                throw new IllegalStateException("Validation fail");
            }
        }
    }
}