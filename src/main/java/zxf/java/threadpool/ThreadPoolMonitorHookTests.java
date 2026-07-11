package zxf.java.threadpool;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 演示通过继承 {@link ThreadPoolExecutor} 重写生命周期钩子，实现线程池运行时监控：
 *
 * <ul>
 *   <li>{@code beforeExecute}：任务执行前回调，可记录开始时间、线程、任务计数。</li>
 *   <li>{@code afterExecute}：任务执行后回调，可统计单任务耗时与异常。</li>
 *   <li>{@code terminated}：线程池终止时回调，可输出汇总指标。</li>
 * </ul>
 *
 * <p>生产环境通常在这些钩子里埋点上报到监控系统（Prometheus、Micrometer 等），
 * 实现对线程池负载、耗时、失败率的可观测性。
 */
public class ThreadPoolMonitorHookTests {

    static class MonitoredThreadPool extends ThreadPoolExecutor {
        private final ThreadLocal<Long> startTime = new ThreadLocal<>();
        private final AtomicLong totalTasks = new AtomicLong();

        MonitoredThreadPool() {
            super(2, 2, 0L, TimeUnit.SECONDS, new ArrayBlockingQueue<>(10));
        }

        @Override
        protected void beforeExecute(Thread t, Runnable r) {
            startTime.set(System.nanoTime());
            System.out.println("  [beforeExecute] " + t.getName() + " 开始任务, 累计提交=" + totalTasks.incrementAndGet());
        }

        @Override
        protected void afterExecute(Runnable r, Throwable t) {
            long durationMs = (System.nanoTime() - startTime.get()) / 1_000_000;
            startTime.remove();
            System.out.println("  [afterExecute ] 任务耗时=" + durationMs + "ms"
                    + (t != null ? ", 异常=" + t : ""));
        }

        @Override
        protected void terminated() {
            System.out.println("  [terminated   ] 线程池已终止, 累计完成任务=" + getCompletedTaskCount());
        }
    }

    public static void main(String[] args) {
        System.out.println(Thread.currentThread() + " ThreadPoolMonitorHookTests.main()");

        MonitoredThreadPool pool = new MonitoredThreadPool();
        for (int i = 1; i <= 3; i++) {
            final int idx = i;
            pool.execute(() -> ThreadPoolUtils.sleep(200 * idx));
        }

        ThreadPoolUtils.shutdown(pool);
    }
}
