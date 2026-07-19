package zxf.java.threadpool;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class ThreadPoolExecutorTests {
    public static void main(String[] args) {
        System.out.println(Thread.currentThread() + " ThreadPoolExecutorTests.main()");

        ThreadPoolExecutor fixedExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(2);

        fixedExecutor.submit(() -> sleepAndPrint(2000, "ThreadPoolExecutorTests.main().fixedExecutor().task1()"));
        fixedExecutor.submit(() -> sleepAndPrint(3000, "ThreadPoolExecutorTests.main().fixedExecutor().task2()"));
        fixedExecutor.submit(() -> sleepAndPrint(3000, "ThreadPoolExecutorTests.main().fixedExecutor().task3()"));

        System.out.println(Thread.currentThread() + " ThreadPoolExecutorTests.main().fixedExecutor() PoolSize: " + fixedExecutor.getPoolSize());
        System.out.println(Thread.currentThread() + " ThreadPoolExecutorTests.main().fixedExecutor() QueueSize: " + fixedExecutor.getQueue().size());

        ThreadPoolExecutor cachedExecutor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        cachedExecutor.submit(() -> sleepAndPrint(3000, "ThreadPoolExecutorTests.main().cachedExecutor().task1()"));
        cachedExecutor.submit(() -> sleepAndPrint(3000, "ThreadPoolExecutorTests.main().cachedExecutor().task2()"));
        cachedExecutor.submit(() -> sleepAndPrint(3000, "ThreadPoolExecutorTests.main().cachedExecutor().task3()"));

        System.out.println(Thread.currentThread() + " ThreadPoolExecutorTests.main().cachedExecutor() PoolSize: " + cachedExecutor.getPoolSize());
        System.out.println(Thread.currentThread() + " ThreadPoolExecutorTests.main().cachedExecutor() QueueSize: " + cachedExecutor.getQueue().size());

        ThreadPoolUtils.shutdown(fixedExecutor);
        ThreadPoolUtils.shutdown(cachedExecutor);
    }

    /** 休眠后打印消息；中断时恢复中断状态并提前返回，避免异常被 submit 静默吞掉。 */
    private static void sleepAndPrint(long millis, String message) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }
        System.out.println(Thread.currentThread() + " " + message);
    }
}
