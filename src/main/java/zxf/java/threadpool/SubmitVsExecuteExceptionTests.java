package zxf.java.threadpool;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

/**
 * 演示 {@code execute} 与 {@code submit} 在任务抛出异常时的关键差异：
 *
 * <ol>
 *   <li><b>execute</b>：任务抛出的未捕获异常会交给线程的
 *       {@link Thread.UncaughtExceptionHandler} 处理（默认打印到 stderr）。</li>
 *   <li><b>submit</b>：异常被封装进 {@link Future}，只有调用 {@code future.get()} 时才会以
 *       {@link java.util.concurrent.ExecutionException} 重新抛出；<b>若无人 get，异常将被静默吞掉</b>。</li>
 * </ol>
 *
 * <p>这正是很多线上任务"无声失败"的根源：用 submit 提交后从不检查 Future。
 */
public class SubmitVsExecuteExceptionTests {

    public static void main(String[] args) throws Exception {
        System.out.println(Thread.currentThread() + " SubmitVsExecuteExceptionTests.main()");

        // 自定义 ThreadFactory：设置 UncaughtExceptionHandler 以捕获 execute 路径的异常
        ThreadFactory factory = r -> {
            Thread t = new Thread(r, "exception-demo");
            t.setUncaughtExceptionHandler((thread, throwable) ->
                    System.out.println("  [UncaughtExceptionHandler] " + thread.getName()
                            + " 捕获异常: " + throwable));
            return t;
        };
        ExecutorService pool = Executors.newSingleThreadExecutor(factory);

        // [1] execute：异常直接由 UncaughtExceptionHandler 捕获
        System.out.println("\n[1] execute 提交一个会抛异常的任务：");
        pool.execute(() -> { throw new RuntimeException("execute-boom"); });
        ThreadPoolUtils.sleep(300); // 等待工作线程触发 handler

        // [2] submit + 不 get：异常封装进 Future，无人 get → 静默丢失
        System.out.println("\n[2] submit 提交会抛异常的任务但不调用 get()：");
        Future<?> silentFuture = pool.submit(() -> { throw new RuntimeException("submit-silent-boom"); });
        ThreadPoolUtils.sleep(300);
        System.out.println("  Future.isDone()=" + silentFuture.isDone() + "，但异常已被吞掉（没有任何输出）");

        // [3] submit + get：通过 get() 重新抛出封装后的 ExecutionException
        System.out.println("\n[3] submit 提交会抛异常的任务并调用 get()：");
        Future<?> loudFuture = pool.submit(() -> { throw new RuntimeException("submit-loud-boom"); });
        try {
            loudFuture.get();
        } catch (Exception e) {
            System.out.println("  get() 抛出: " + e.getClass().getSimpleName()
                    + "，cause=" + e.getCause());
        }

        System.out.println("\n结论：提交任务后务必处理失败路径——"
                + "execute 配 UncaughtExceptionHandler，submit 必须检查 Future。");
        ThreadPoolUtils.shutdown(pool);
    }
}
