package levin.learn.perf4j;

import java.util.Random;

import org.perf4j.log4j.Log4JStopWatch;

public class SimplePerfCollector {
    public static void main(String[] args) {
        for (int i = 0; i < 20; i++) {
            Log4JStopWatch stopWatch = new Log4JStopWatch();
            stopWatch.setTimeThreshold(50);
            stopWatch.setNormalAndSlowSuffixesEnabled(true);
            methodToMeasure();
            if (i % 2 == 0) {
                stopWatch.stop("methodToMeasure");
            } else {
                stopWatch.stop("methodToMeasureException", new Exception("test"));
            }
        }
        //output: 
        // 150418 19:12:21,764 [INFO ] [main] o.p.TimingLogger - start[1429355541719] time[44] tag[methodToMeasure.normal]
        // 150418 19:12:21,812 [WARN ] [main] o.p.TimingLogger - start[1429355541765] time[47] tag[methodToMeasureException.normal]
        // java.lang.Exception: test
        //      at levin.learn.perf4j.SimplePerfCollector.main(SimplePerfCollector.java:17)
    }
    
    private static void methodToMeasure() {
        try {
            Thread.sleep(new Random().nextInt(100));
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
