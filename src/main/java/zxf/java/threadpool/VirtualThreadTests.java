package zxf.java.threadpool;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CountDownLatch;

/**
 * JDK 21 虚拟线程（Virtual Thread, JEP 444）演示。
 *
 * <p>对比同一批大量"含少量等待（模拟 IO）"的短任务，在两种执行器下的吞吐：
 * <ul>
 *   <li>固定大小平台线程池（200 个 OS 线程）</li>
 *   <li>{@link Executors#newVirtualThreadPerTaskExecutor()}（每任务一个虚拟线程）</li>
 * </ul>
 *
 * <p>虚拟线程是轻量级线程，挂起时不占用 OS 线程，因而能在 IO 等待场景下
 * 用极少的载体线程承载海量并发任务，吞吐显著高于平台线程池。
 *
 * <p>这是升级到 JDK 21 的核心收益之一。
 */
public class VirtualThreadTests {

    private static final int TASK_COUNT = 10_000;

    public static void main(String[] args) throws InterruptedException {
        System.out.println(Thread.currentThread() + " VirtualThreadTests.main()");
        System.out.println("任务总数: " + TASK_COUNT + "，每个任务含 10ms 模拟 IO 等待\n");

        ThreadPoolExecutor platform = new ThreadPoolExecutor(
                200, 200, 0L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                ThreadPoolUtils.namedThreadFactory("platform"));
        long platformMs = run(platform, "platform-fixed-200");
        long platformPeak = platform.getLargestPoolSize();
        ThreadPoolUtils.shutdown(platform);

        ExecutorService virtual = Executors.newVirtualThreadPerTaskExecutor();
        long virtualMs = run(virtual, "virtual-thread");
        ThreadPoolUtils.shutdown(virtual);

        System.out.println("\n===== 结论 =====");
        System.out.println("平台线程池  : " + platformMs + " ms, 峰值 OS 线程数 ≈ " + platformPeak);
        System.out.println("虚拟线程    : " + virtualMs + " ms, 峰值虚拟线程数 ≈ " + TASK_COUNT + "（每任务一线程，轻量不占 OS 线程）");
    }

    /** 提交 taskCount 个短任务，全部完成（通过 CountDownLatch 等待）后返回耗时（毫秒）。 */
    private static long run(ExecutorService executor, String label) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(TASK_COUNT);
        long start = System.nanoTime();
        for (int i = 0; i < TASK_COUNT; i++) {
            executor.submit(() -> {
                ThreadPoolUtils.sleep(10); // 模拟 IO 等待
                latch.countDown();
                return null;
            });
        }
        latch.await();
        long ms = (System.nanoTime() - start) / 1_000_000;
        System.out.println(label + " 完成，耗时 " + ms + " ms");
        return ms;
    }
}
