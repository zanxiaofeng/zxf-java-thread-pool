package zxf.java.threadpool;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 演示 {@link ThreadPoolExecutor} 的 4 种内置拒绝策略（RejectedExecutionHandler）：
 *
 * <ol>
 *   <li><b>AbortPolicy</b>（默认）：直接抛出 RejectedExecutionException。</li>
 *   <li><b>CallerRunsPolicy</b>：让提交任务的线程（调用者）自己执行该任务，起到背压降速作用。</li>
 *   <li><b>DiscardPolicy</b>：静默丢弃新任务，无任何提示（容易埋坑）。</li>
 *   <li><b>DiscardOldestPolicy</b>：丢弃队列头部（最早）的任务，再次尝试提交新任务。</li>
 * </ol>
 *
 * <p>线程池配置：core=max=1, queue=ArrayBlockingQueue(1)，最多承载 2 个任务，第 3 个触发拒绝。
 */
public class RejectedExecutionHandlerTests {

    public static void main(String[] args) {
        System.out.println(Thread.currentThread() + " RejectedExecutionHandlerTests.main()");

        test("AbortPolicy", new ThreadPoolExecutor.AbortPolicy());
        test("CallerRunsPolicy", new ThreadPoolExecutor.CallerRunsPolicy());
        test("DiscardPolicy", new ThreadPoolExecutor.DiscardPolicy());
        test("DiscardOldestPolicy", new ThreadPoolExecutor.DiscardOldestPolicy());
    }

    private static void test(String name, RejectedExecutionHandler handler) {
        System.out.println("\n===== " + name + " =====");
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                1, 1, 0L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(1),
                ThreadPoolUtils.namedThreadFactory(name),
                handler);

        for (int i = 1; i <= 3; i++) {
            final int idx = i;
            try {
                executor.execute(() -> {
                    System.out.println("  [task-" + idx + "] running in " + Thread.currentThread());
                    ThreadPoolUtils.sleep(500);
                });
                System.out.println("  submitted task-" + idx);
            } catch (RejectedExecutionException e) {
                System.out.println("  task-" + idx + " rejected (AbortPolicy 抛异常)");
            }
        }

        ThreadPoolUtils.shutdown(executor);
    }
}
