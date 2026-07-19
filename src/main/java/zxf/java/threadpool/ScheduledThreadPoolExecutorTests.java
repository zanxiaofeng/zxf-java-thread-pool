package zxf.java.threadpool;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 演示 {@link ScheduledExecutorService} 的三种调度方式：
 *
 * <ol>
 *   <li><b>schedule</b>：一次性延迟任务，延迟指定时间后执行一次。</li>
 *   <li><b>scheduleAtFixedRate</b>：固定速率周期执行——以上一次<b>开始时间</b>为基准，
 *       任务执行慢于周期时会追赶执行。</li>
 *   <li><b>scheduleWithFixedDelay</b>：固定延迟周期执行——以上一次<b>结束时间</b>为基准，
 *       每次执行完再等待指定延迟。</li>
 * </ol>
 *
 * <p>周期任务返回的 {@link ScheduledFuture} 可通过 {@code cancel(true)} 取消后续执行。
 */
public class ScheduledThreadPoolExecutorTests {
    public static void main(String[] args) throws InterruptedException {
        System.out.println(Thread.currentThread() + " ScheduledThreadPoolExecutorTests.main()");

        ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(2);

        // schedule：100ms 后执行一次（延迟必须短于整个 demo 的运行时长，否则任务会在 shutdown 时被丢弃）
        scheduledExecutor.schedule(() -> {
            System.out.println(Thread.currentThread() + " ScheduledThreadPoolExecutorTests.main().scheduledExecutor().schedule()");
        }, 100, TimeUnit.MILLISECONDS);

        CountDownLatch scheduleAtFixedRateCountDownLatch = new CountDownLatch(3);
        ScheduledFuture<?> scheduleAtFixedRateFuture = scheduledExecutor.scheduleAtFixedRate(() -> {
            System.out.println(Thread.currentThread() + " ScheduledThreadPoolExecutorTests.main().scheduledExecutor().scheduleAtFixedRate()");
            scheduleAtFixedRateCountDownLatch.countDown();
        }, 500, 100, TimeUnit.MILLISECONDS);
        scheduleAtFixedRateCountDownLatch.await(1000, TimeUnit.MILLISECONDS);
        scheduleAtFixedRateFuture.cancel(true);

        CountDownLatch scheduleWithFixedDelayCountDownLatch = new CountDownLatch(3);
        ScheduledFuture<?> scheduleWithFixedDelayFuture = scheduledExecutor.scheduleWithFixedDelay(() -> {
            System.out.println(Thread.currentThread() + " ScheduledThreadPoolExecutorTests.main().scheduledExecutor().scheduleWithFixedDelay()");
            scheduleWithFixedDelayCountDownLatch.countDown();
        }, 100, 150, TimeUnit.MILLISECONDS);
        scheduleWithFixedDelayCountDownLatch.await(1000, TimeUnit.MILLISECONDS);
        scheduleWithFixedDelayFuture.cancel(true);

        ThreadPoolUtils.shutdown(scheduledExecutor);
    }
}
