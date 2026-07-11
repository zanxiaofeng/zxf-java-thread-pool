package zxf.java.threadpool;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 演示 {@link CompletableFuture} 常用的异步编排能力：
 *
 * <ol>
 *   <li><b>thenApply</b>：对上一步结果做同步转换（同链路）。</li>
 *   <li><b>thenCompose</b>：串联一个异步依赖（返回另一个 future，避免 CompletableFuture&lt;CompletableFuture&gt;）。</li>
 *   <li><b>allOf</b>：等待所有任务完成。</li>
 *   <li><b>anyOf</b>：任一任务完成即返回。</li>
 *   <li><b>exceptionally</b>：异常兜底恢复。</li>
 * </ol>
 *
 * <p>说明：所有异步操作都显式传入线程池，避免默认使用 ForkJoinPool.commonPool() 难以管控。
 */
public class CompletableFutureTests {

    public static void main(String[] args) throws Exception {
        System.out.println(Thread.currentThread() + " CompletableFutureTests.main()");

        ExecutorService pool = Executors.newFixedThreadPool(4);

        // 1. thenApply：同步转换结果
        CompletableFuture<String> f1 = CompletableFuture
                .supplyAsync(() -> {
                    ThreadPoolUtils.sleep(100);
                    return 100;
                }, pool)
                .thenApply(n -> "value=" + n);
        System.out.println("thenApply      => " + f1.get());

        // 2. thenCompose：串联异步依赖（flatMap 语义）
        CompletableFuture<String> f2 = CompletableFuture
                .supplyAsync(() -> {
                    ThreadPoolUtils.sleep(100);
                    return "data";
                }, pool)
                .thenCompose(data -> CompletableFuture.supplyAsync(() -> data + "-processed", pool));
        System.out.println("thenCompose    => " + f2.get());

        // 3. allOf：等待全部完成
        CompletableFuture<String> a = CompletableFuture.supplyAsync(() -> { ThreadPoolUtils.sleep(150); return "A"; }, pool);
        CompletableFuture<String> b = CompletableFuture.supplyAsync(() -> { ThreadPoolUtils.sleep(100); return "B"; }, pool);
        CompletableFuture.allOf(a, b).get();
        System.out.println("allOf          => a=" + a.get() + ", b=" + b.get());

        // 4. anyOf：任一完成即返回（此处 fast 先完成）
        CompletableFuture<Object> any = CompletableFuture.anyOf(
                CompletableFuture.supplyAsync(() -> { ThreadPoolUtils.sleep(200); return "slow"; }, pool),
                CompletableFuture.supplyAsync(() -> { ThreadPoolUtils.sleep(50); return "fast"; }, pool));
        System.out.println("anyOf          => " + any.get());

        // 5. exceptionally：异常兜底
        CompletableFuture<String> recovered = CompletableFuture
                .<String>supplyAsync(() -> {
                    throw new RuntimeException("boom");
                }, pool)
                .exceptionally(ex -> "recovered: " + ex.getCause().getMessage());
        System.out.println("exceptionally  => " + recovered.get());

        ThreadPoolUtils.shutdown(pool);
    }
}
