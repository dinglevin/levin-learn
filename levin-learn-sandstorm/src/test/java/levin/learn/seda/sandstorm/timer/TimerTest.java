package levin.learn.seda.sandstorm.timer;

import org.junit.Test;

import seda.sandstorm.api.EventElement;
import seda.sandstorm.core.EventQueueImpl;
import seda.sandstorm.timer.Timer;
import seda.sandstorm.timer.TimerHandle;

public class TimerTest {
    @Test
    public void testTimerEvent() {
        EventQueueImpl eventQueue = new EventQueueImpl("timer");
        Timer timer = new Timer();

        timer.registerEvent(1, new TestEvent("1"), eventQueue);
        timer.registerEvent(10, new TestEvent("10"), eventQueue);
        timer.registerEvent(20, new TestEvent("20"), eventQueue);
        timer.registerEvent(30, new TestEvent("30"), eventQueue);
        timer.registerEvent(40, new TestEvent("40"), eventQueue);
        timer.registerEvent(50, new TestEvent("50"), eventQueue);
        timer.registerEvent(250, new TestEvent("250"), eventQueue);
        timer.registerEvent(500, new TestEvent("500"), eventQueue);
        TimerHandle handle1 = timer.registerEvent(2500, new TestEvent("2500"), eventQueue);
        timer.registerEvent(1500, new TestEvent("1500"), eventQueue);
        timer.registerEvent(3500, new TestEvent("3500"), eventQueue);
        TimerHandle handle2 = timer.registerEvent(15000, new TestEvent("15000"), eventQueue);
        timer.registerEvent(8000, new TestEvent("8000"), eventQueue);

        int numGot = 0;
        while (numGot < 13) {
            EventElement events[] = eventQueue.dequeueAll();
            if (events != null) {
                numGot += events.length;
                System.out.println("got " + events.length + " event" + (events.length > 1 ? "s" : ""));
                for (int i = 0; i < events.length; i++)
                    System.out.println("  " + i + ": " + events[i]);
                System.out.println("total num got so far is: " + numGot);
                System.out.println("num remain is: " + timer.size());
                if (numGot >= 3 && handle1.isActive()) {
                    System.out.println("Canncel handle1");
                    timer.cancelEvent(handle1);
                    numGot++;
                }

                if (numGot >= 5 && handle2.isActive()) {
                    System.out.println("Canncel handle2");
                    timer.cancelEvent(handle2);
                    numGot++;
                }
            } else {
                try {
                    Thread.sleep(5);
                } catch (InterruptedException ie) {
                }
            }
        }

        System.out.println("Done with timer");
        timer.doneWithTimer();
    }
    
    private static class TestEvent implements EventElement {
        private String ns = null;
        private long inj;

        public TestEvent(String f) {
            ns = f;
            inj = System.currentTimeMillis();
        }

        public String toString() {
            return ns + " elapsed=" + (System.currentTimeMillis() - inj);
        }
    }
}
