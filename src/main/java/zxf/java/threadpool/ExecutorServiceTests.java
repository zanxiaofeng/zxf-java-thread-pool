package zxf.java.threadpool;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * 演示 {@link ExecutorService} 的核心 API：
 *
 * <ul>
 *   <li><b>execute</b>：提交 Runnable，无返回值。</li>
 *   <li><b>submit</b>：提交 Callable，返回 {@link Future} 获取结果。</li>
 *   <li><b>invokeAny</b>：提交一批任务，返回<b>任一</b>最先成功完成的结果，其余任务被取消（中断）。</li>
 *   <li><b>invokeAll</b>：提交一批任务，阻塞等待<b>全部</b>完成，按提交顺序返回 Future 列表。</li>
 * </ul>
 */
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

        List<Callable<String>> callables = List.of(() -> {
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

        // invokeAny：callable-1 耗时最短（1s），返回其结果，callable-2/3 被取消（中断）
        String invokeAny = executorService.invokeAny(callables);
        System.out.println(Thread.currentThread() + "  invokeAny => " + invokeAny);

        // invokeAll：阻塞等待全部完成（2 线程池下约 4s），futures 按提交顺序排列
        List<Future<String>> futures = executorService.invokeAll(callables);
        for (int i = 0; i < futures.size(); i++) {
            System.out.println(Thread.currentThread() + "  invokeAll => futures[" + i + "] = " + futures.get(i).get());
        }

        ThreadPoolUtils.shutdown(executorService);
    }
}
