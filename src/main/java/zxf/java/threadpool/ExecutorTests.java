package zxf.java.threadpool;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ExecutorTests {
    public static void main(String[] args) {
        System.out.println(Thread.currentThread() + " ExecutorTests.main()");

        // ExecutorService 继承自 Executor，这里用 Executor 视角只调用 execute()
        ExecutorService executor1 = Executors.newSingleThreadExecutor();
        executor1.execute(() -> {
            System.out.println(Thread.currentThread() + "  ExecutorTests.main().executor1()");
        });

        ExecutorService executor2 = Executors.newSingleThreadExecutor(Executors.defaultThreadFactory());
        executor2.execute(() -> {
            System.out.println(Thread.currentThread() + "  ExecutorTests.main().executor2()");
        });

        shutdown(executor1);
        shutdown(executor2);
    }

    private static void shutdown(ExecutorService executor) {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
