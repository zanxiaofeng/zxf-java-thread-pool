package zxf.java.threadpool;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class ThreadPoolExecutorTests {
    public static void main(String[] args) {
        System.out.println(Thread.currentThread() + " ThreadPoolExecutorTests.main()");

        ThreadPoolExecutor fixedExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(2);

        fixedExecutor.submit(() -> {
            Thread.sleep(2000);
            System.out.println(Thread.currentThread() + " ThreadPoolExecutorTests.main().fixedExecutor().task1()");
            return null;
        });
        fixedExecutor.submit(() -> {
            Thread.sleep(3000);
            System.out.println(Thread.currentThread() + " ThreadPoolExecutorTests.main().fixedExecutor().task2()");
            return null;
        });
        fixedExecutor.submit(() -> {
            Thread.sleep(3000);
            System.out.println(Thread.currentThread() + " ThreadPoolExecutorTests.main().fixedExecutor().task3()");
            return null;
        });

        System.out.println(Thread.currentThread() + " ThreadPoolExecutorTests.main().fixedExecutor() PoolSize: " + fixedExecutor.getPoolSize());
        System.out.println(Thread.currentThread() + " ThreadPoolExecutorTests.main().fixedExecutor() QueueSize: " + fixedExecutor.getQueue().size());

        ThreadPoolExecutor cachedExecutor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        cachedExecutor.submit(() -> {
            Thread.sleep(3000);
            System.out.println(Thread.currentThread() + " ThreadPoolExecutorTests.main().cachedExecutor().task1()");
            return null;
        });
        cachedExecutor.submit(() -> {
            Thread.sleep(3000);
            System.out.println(Thread.currentThread() + " ThreadPoolExecutorTests.main().cachedExecutor().task2()");
            return null;
        });
        cachedExecutor.submit(() -> {
            Thread.sleep(3000);
            System.out.println(Thread.currentThread() + " ThreadPoolExecutorTests.main().cachedExecutor().task3()");
            return null;
        });

        System.out.println(Thread.currentThread() + " ThreadPoolExecutorTests.main().cachedExecutor() PoolSize: " + cachedExecutor.getPoolSize());
        System.out.println(Thread.currentThread() + " ThreadPoolExecutorTests.main().cachedExecutor() QueueSize: " + cachedExecutor.getQueue().size());
    }
}
