package zxf.java.threadpool;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExecutorTests {
    public static void main(String[] args) {
        System.out.println(Thread.currentThread() + " ExecutorTests.main()");

        // ExecutorService 继承自 Executor，这里用 Executor 视角只调用 execute()
        ExecutorService executor1 = Executors.newSingleThreadExecutor();
        executor1.execute(() -> {
            System.out.println(Thread.currentThread() + "  ExecutorTests.main().executor1()");
        });

        // 与无参版本等价，此处仅演示可传入自定义 ThreadFactory 的重载签名
        ExecutorService executor2 = Executors.newSingleThreadExecutor(Executors.defaultThreadFactory());
        executor2.execute(() -> {
            System.out.println(Thread.currentThread() + "  ExecutorTests.main().executor2()");
        });

        ThreadPoolUtils.shutdown(executor1);
        ThreadPoolUtils.shutdown(executor2);
    }
}
