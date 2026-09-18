package zxf.java.threadpool;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 线程池 demo 共享工具方法。
 * 仅用于简化示例代码（休眠、优雅关闭、线程命名工厂），不含业务逻辑。
 */
public final class ThreadPoolUtils {

    private ThreadPoolUtils() {
    }

    /**
     * 休眠指定毫秒（演示用）。
     * 被中断时恢复中断状态并提前返回，避免抛出 InterruptedException 打断 demo 流程。
     */
    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 优雅关闭线程池：先 {@link ExecutorService#shutdown()} 停止接收新任务，
     * 等待最多 10 秒，超时则 {@link ExecutorService#shutdownNow()} 强制终止。
     */
    public static void shutdown(ExecutorService executor) {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 创建带命名前缀的线程工厂，便于从线程名识别任务来源（如 demo-thread-1）。
     */
    public static ThreadFactory namedThreadFactory(String prefix) {
        AtomicInteger counter = new AtomicInteger();
        return r -> new Thread(r, prefix + "-" + counter.incrementAndGet());
    }
}
