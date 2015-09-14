package levin.learn.seda.disk;

import org.junit.Test;

import seda.sandstorm.core.BufferEvent;
import seda.sandstorm.core.EventQueueImpl;
import seda.sandstorm.lib.disk.AsyncFile;

public class FileWriterTest {
    @Test
    public void testSimpleWriter() throws Exception {
        EventQueueImpl eventQueue = new EventQueueImpl("async.file.test");
        AsyncFile file = new AsyncFile("test.txt", eventQueue, true, false);
        for (int i = 0; i < 10; i++) {
            BufferEvent bufEvent = new BufferEvent(("Test" + i).getBytes());
            file.write(bufEvent);
        }
        
        file.close();
    }
}
