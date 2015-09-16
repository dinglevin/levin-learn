package levin.learn.seda.sandstorm.disk;

import java.util.Arrays;

import org.junit.Test;

import seda.sandstorm.api.EventElement;
import seda.sandstorm.core.BufferEvent;
import seda.sandstorm.core.EventQueueImpl;
import seda.sandstorm.lib.disk.AsyncFile;

public class FileWriterTest {
    @Test
    public void testSimpleWriter() throws Exception {
        EventQueueImpl eventQueue = new EventQueueImpl("async.file.test");
        AsyncFile file = new AsyncFile("target/test.txt", eventQueue, true, false);
        for (int i = 0; i < 10; i++) {
            BufferEvent bufEvent = new BufferEvent(("Test" + i + "\n").getBytes());
            file.write(bufEvent);
        }
        
        Thread.sleep(10000);
        
        EventElement[] events = eventQueue.blockingDequeueAll(10000);
        System.out.println("Events: " + Arrays.toString(events));
        
        file.close();
    }
}
