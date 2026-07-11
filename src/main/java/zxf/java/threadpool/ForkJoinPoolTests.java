package zxf.java.threadpool;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

/**
 * 演示 {@link ForkJoinPool} 的分治（divide-and-conquer）模型与 work-stealing 窃取机制。
 *
 * <p>{@link RecursiveTask} 表示有返回值的分治任务：
 * <ol>
 *   <li>数据量低于阈值 → 直接计算（递归终止条件）。</li>
 *   <li>否则把任务一分为二，{@code left.fork()} 异步执行左半，{@code right.compute()} 同步执行右半，
 *       最后 {@code left.join()} 合并结果。</li>
 * </ol>
 *
 * <p>work-stealing：每个 worker 线程有自己的双端队列；自己任务从 LIFO 取（充分利用缓存），
 * 空闲时从其他线程队列尾部 FIFO 偷取，从而负载均衡。{@code Executors.newWorkStealingPool()} 底层就是它。
 *
 * <p>注意：ForkJoinPool 适合 CPU 密集型可拆分任务；不适合含大量阻塞/IO 的任务。
 */
public class ForkJoinPoolTests {

    private static final int THRESHOLD = 10_000;

    /** 对数组区间 [from, to) 求和的分治任务。 */
    static class SumTask extends RecursiveTask<Long> {
        private final long[] numbers;
        private final int from;
        private final int to;

        SumTask(long[] numbers, int from, int to) {
            this.numbers = numbers;
            this.from = from;
            this.to = to;
        }

        @Override
        protected Long compute() {
            if (to - from <= THRESHOLD) {
                long sum = 0;
                for (int i = from; i < to; i++) {
                    sum += numbers[i];
                }
                return sum;
            }
            int mid = (from + to) >>> 1;
            SumTask left = new SumTask(numbers, from, mid);
            SumTask right = new SumTask(numbers, mid, to);
            left.fork(); // 异步执行左半部分
            return right.compute() + left.join(); // 同步右半 + 等待左半合并
        }
    }

    public static void main(String[] args) {
        System.out.println(Thread.currentThread() + " ForkJoinPoolTests.main()");

        int n = 1_000_000;
        long[] numbers = new long[n];
        for (int i = 0; i < n; i++) {
            numbers[i] = i + 1;
        }

        ForkJoinPool pool = new ForkJoinPool();
        long start = System.nanoTime();
        long sum = pool.invoke(new SumTask(numbers, 0, n));
        long ms = (System.nanoTime() - start) / 1_000_000;

        long expected = (long) n * (n + 1) / 2;
        System.out.println("并行度=" + pool.getParallelism()
                + ", ForkJoin 求和=" + sum
                + " (期望 " + expected + ", 校验" + (sum == expected ? "通过" : "失败") + ")"
                + ", 耗时=" + ms + "ms");

        pool.shutdown();
    }
}
