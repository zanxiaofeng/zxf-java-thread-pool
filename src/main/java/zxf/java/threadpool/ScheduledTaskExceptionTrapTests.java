package zxf.java.threadpool;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 演示周期任务（scheduleAtFixedRate / scheduleWithFixedDelay）的经典陷阱：
 * <b>某次执行抛出未捕获异常，会导致后续所有周期任务被静默取消。</b>
 *
 * <p>原因：周期任务的调度依赖上一次正常完成；一旦抛异常，下次执行不再被安排，
 * 且没有任何告警，极易造成"定时任务悄悄停摆"的生产事故。
 *
 * <p>对策：在任务体内 try/catch 所有可抛出的异常，绝不让异常外泄。
 */
public class ScheduledTaskExceptionTrapTests {

    public static void main(String[] args) throws InterruptedException {
        System.out.println(Thread.currentThread() + " ScheduledTaskExceptionTrapTests.main()");

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        CountDownLatch latch = new CountDownLatch(5); // 期望成功执行 5 次

        // 初始延迟 0，固定周期 200ms；第 3 次抛出异常
        scheduler.scheduleAtFixedRate(() -> {
            long count = 5 - latch.getCount() + 1;
            System.out.println("  第 " + count + " 次执行 in " + Thread.currentThread());
            if (count == 3) {
                throw new RuntimeException("周期任务抛异常 → 后续将不再执行！");
            }
            latch.countDown();
        }, 0, 200, TimeUnit.MILLISECONDS);

        // 等待 2 秒，观察能否执行满 5 次
        boolean reached5 = latch.await(2, TimeUnit.SECONDS);
        System.out.println("2 秒后是否执行满 5 次? " + reached5
                + "（实际成功次数 = " + (5 - latch.getCount()) + "）");
        System.out.println("结论：周期任务抛出未捕获异常后，后续周期执行被静默取消。");
        System.out.println("对策：任务体内 try/catch 所有异常，确保周期不被中断。");

        ThreadPoolUtils.shutdown(scheduler);
    }
}
