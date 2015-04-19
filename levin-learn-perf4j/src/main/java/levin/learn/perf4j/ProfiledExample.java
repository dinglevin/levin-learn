package levin.learn.perf4j;

import org.perf4j.aop.Profiled;

public class ProfiledExample {
    public static void main (String[] args) throws Exception {
        // run all of the examples
        for (int i = 0; i < 50; i++) {
            simpleExample();
            simpleExampleWithExplicitTag();
            dynamicTagExample("iIs" + (i % 2 == 0 ? "Even" : "Odd"));
            dynamicMessageExample(i % 2 == 0 ? 1000L : 2000L);
            if (i % 2 == 0) {
                logFailuresSeparatelyExample(false);
            } else {
                try {
                    logFailuresSeparatelyExample(true);
                } catch (Exception e) { /* expected */ }
            }
            nonDefaultLoggerNameExample();

            System.out.println("Ran loop " + i);
        }
    }

    /**
     * When the Profiled annotation is used without any parameters, the simple
     * name of the method is used as the tag for any logged timing statements
     */
    @Profiled
    public static void simpleExample() throws InterruptedException {
        Thread.sleep((long) (Math.random() * 1000L));
    }

    /**
     * Here we set the tag name to an explicit value.
     */
    @Profiled(tag = "simpleBlock")
    public static void simpleExampleWithExplicitTag() throws InterruptedException {
        Thread.sleep((long) (Math.random() * 1000L));
    }

    /**
     * One very useful feature of the Profiled annotation is the ability to
     * dynamically set the tag name based on the value of a method parameter.
     * You can make use of Jakarta Commons Java Expression Language (JEXL) in
     * curly braces in the tag name. The first parameter is assigned the variable
     * name $0, the second $1, etc. For example, if I had a method that took as
     * the first parameter an object with a bean accessor method "getFoo()", I
     * could write a tag as "myTag_{$0.foo}".
     *
     * In addition to method parameters, you can also access the following variables
     * in JEXL expressions:
     *
     * $this - the object whose method is being called.
     * $return - the return value from the method (this will be null if the method
     *           has a void return type or if the method throws an exception)
     * $exception - if the method completes by throwing an exception, the $exception
     *              variable can be used to access the exception (this will be null
     *              if the method completes normlly)
     *
     */
    @Profiled(tag = "dynamicTag_{$0}")
    public static void dynamicTagExample(String tagName) throws InterruptedException {
        Thread.sleep((long) (Math.random() * 1000L));
    }

    /**
     * You can also specify a message to be logged in the Profiled annotation.
     */
    @Profiled(tag = "messageExample", message = "Requested sleep time was {$0 / 1000} seconds")
    public static void dynamicMessageExample(long requestedSleepTimeInMS) throws InterruptedException {
        Thread.sleep((long) (Math.random() * requestedSleepTimeInMS));
    }

    /**
     * Often times you want to log failures (where a method completes by throwing an exception)
     * separately from when a method completes normally. If you set the logFailuresSeparately
     * parameter to true, then the tag given to cases when the method completes normally will
     * be yourSpecifiedTagName.success, and when an exception is thrown the tag name will be
     * yourSpecifiedTagName.failure. This allows you to calculate separate performance statistics
     * for success and failure cases.
     */
    @Profiled(tag = "failuresSeparatelyExample", logFailuresSeparately = true)
    public static void logFailuresSeparatelyExample(boolean shouldFail) throws Exception {
        Thread.sleep((long) (Math.random() * 1000L));
        if (shouldFail) {
            throw new Exception("Method threw exception");
        }
    }

    /**
     * By default timing statements are logged to the logger named
     * StopWatch.DEFAULT_LOGGER_NAME (i.e. org.perf4j.TimingLogger),
     * but this can be overridden.
     */
    @Profiled(tag = "loggerExample", logger = "org.perf4j.MyCustomLoggerName")
    public static void nonDefaultLoggerNameExample() throws InterruptedException {
        Thread.sleep((long) (Math.random() * 1000L));
    }
}
