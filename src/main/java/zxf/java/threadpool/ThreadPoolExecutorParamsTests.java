package zxf.java.threadpool;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 演示手动构造 {@link ThreadPoolExecutor} 的 7 大核心参数，以及任务提交后的扩容流程：
 *
 * <p>核心 → 队列 → 最大线程数 → 拒绝策略
 *
 * <p>本例配置：corePoolSize=2, maximumPoolSize=4, workQueue=ArrayBlockingQueue(2)，
 * 因此最多承载 core + queue + (max - core) = 2 + 2 + 2 = 6 个任务，第 7 个触发拒绝。
 *
 * <p>注意：阿里规约禁止用 Executors.newFixedThreadPool/newCachedThreadPool（队列或线程无界易 OOM），
 * 生产环境应像本例一样手动构造，明确每一条边界。
 */
public class ThreadPoolExecutorParamsTests {

    public static void main(String[] args) {
        System.out.println(Thread.currentThread() + " ThreadPoolExecutorParamsTests.main()");

        // 7 大参数：core, max, keepAliveTime, unit, workQueue, threadFactory, rejectedHandler
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                2,                                      // corePoolSize：核心线程数
                4,                                      // maximumPoolSize：最大线程数
                10L, TimeUnit.SECONDS,                  // keepAliveTime + unit：非核心线程空闲存活时间
                new ArrayBlockingQueue<>(2),            // workQueue：有界任务队列
                Executors.defaultThreadFactory(),       // threadFactory
                new ThreadPoolExecutor.AbortPolicy());  // rejectedExecutionHandler：默认拒绝策略

        // 每个任务 sleep 5s 占住线程，便于观察扩容过程
        for (int i = 1; i <= 7; i++) {
            submitTask(executor, i);
            ThreadPoolUtils.sleep(200); // 给线程创建/调度留出时间，让快照更准确
        }

        ThreadPoolUtils.shutdown(executor);
    }

    private static void submitTask(ThreadPoolExecutor executor, int index) {
        try {
            executor.execute(() -> {
                System.out.println(Thread.currentThread() + "  running task-" + index);
                ThreadPoolUtils.sleep(5_000);
            });
            System.out.println(Thread.currentThread() + "  submitted task-" + index
                    + " => poolSize=" + executor.getPoolSize()
                    + ", activeCount=" + executor.getActiveCount()
                    + ", queueSize=" + executor.getQueue().size());
        } catch (RejectedExecutionException e) {
            // core 满(2) → 队列满(2) → max 满(4) 之后，第 7 个任务触发 AbortPolicy 拒绝
            System.out.println(Thread.currentThread() + "  task-" + index + " REJECTED"
                    + " => poolSize=" + executor.getPoolSize()
                    + ", queueSize=" + executor.getQueue().size());
        }
    }
}
