package zxf.java.threadpool;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;

public class ExecutorServiceTests {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        System.out.println(Thread.currentThread() + " ExecutorServiceTests.main()");

        ExecutorService executorService = Executors.newFixedThreadPool(2);

        executorService.execute(() -> {
            System.out.println(Thread.currentThread() + "  ExecutorServiceTests.main().executorService().execute()");
        });

        Future<String> submitFuture = executorService.submit(() -> {
            System.out.println(Thread.currentThread() + "  ExecutorServiceTests.main().executorService().submit()");
            return "submit-1";
        });
        System.out.println(Thread.currentThread() + "  ExecutorServiceTests.main().executorService().submit().future, " + submitFuture.get());

        List<Callable<String>> callables = Arrays.asList(() -> {
            System.out.println(Thread.currentThread() + "  ExecutorServiceTests.main().executorService().callable1()");
            Thread.sleep(1000);
            return "callable-1";
        }, () -> {
            System.out.println(Thread.currentThread() + "  ExecutorServiceTests.main().executorService().callable2()");
            Thread.sleep(2000);
            return "callable-2";
        }, () -> {
            System.out.println(Thread.currentThread() + "  ExecutorServiceTests.main().executorService().callable3()");
            Thread.sleep(3000);
            return "callable-3";
        });

        String invokeAny = executorService.invokeAny(callables);
        List<Future<String>> futures = executorService.invokeAll(callables);

        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                List<Runnable> notExecutedTasks = executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
    }
}
