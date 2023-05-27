package zxf.java.threadpool;

import java.util.List;
import java.util.concurrent.*;

public class ScheduledThreadPoolExecutorTests {
    public static void main(String[] args) throws InterruptedException {
        System.out.println(Thread.currentThread() + " ScheduledThreadPoolExecutorTests.main()");

        ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(2);

        scheduledExecutor.schedule(() -> {
            System.out.println(Thread.currentThread() + " ScheduledThreadPoolExecutorTests.main().scheduledExecutor().schedule()");
        }, 30, TimeUnit.SECONDS);


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

        scheduledExecutor.shutdown();
        try {
            if (!scheduledExecutor.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                List<Runnable> notExecutedTasks = scheduledExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduledExecutor.shutdownNow();
        }
    }
}
