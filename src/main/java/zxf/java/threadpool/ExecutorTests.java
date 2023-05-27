package zxf.java.threadpool;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ExecutorTests {
    public static void main(String[] args) {
        System.out.println(Thread.currentThread() + " ExecutorTests.main()");

        Executor executor1 = Executors.newSingleThreadExecutor();
        executor1.execute(() -> {
            System.out.println(Thread.currentThread() + "  ExecutorTests.main().executor1()");
        });

        Executor executor2 = Executors.newSingleThreadExecutor(Executors.defaultThreadFactory());
        executor2.execute(() -> {
            System.out.println(Thread.currentThread() + "  ExecutorTests.main().executor2()");
        });
    }
}