# zxf-java-thread-pool

Java 并发线程池（Thread Pool）学习与演示项目，基于 **JDK 21**，通过一系列可独立运行的 `main` 程序，系统化演示 `java.util.concurrent` 线程池的核心 API、内部机制、陷阱与最佳实践。

## 目录

- [环境要求](#环境要求)
- [快速开始](#快速开始)
- [示例清单](#示例清单)
- [线程池知识体系](#线程池知识体系)
- [ThreadPoolExecutor 工作原理](#threadpoolexecutor-工作原理)
- [最佳实践](#最佳实践)
- [JDK 21 虚拟线程](#jdk-21-虚拟线程)
- [常见陷阱](#常见陷阱)

---

## 环境要求

- **JDK 21**（项目使用 `maven.compiler.release=21`；需完整 JDK——仅安装 JRE 或 `javac` 版本过低会报 `release version 21 not supported`）
- **Maven 3.6+**

> 本项目用到 JDK 21 的 `Executors.newVirtualThreadPerTaskExecutor()`（[JEP 444](https://openjdk.org/jeps/444)）。
>
> 若 `mvn clean compile` 报 "release version 21 not supported"，说明 `JAVA_HOME` 指向了 JRE 或低版本 JDK，请将其指向完整的 JDK 21 后重试。

## 快速开始

```bash
# 编译
mvn clean compile

# 运行任意一个示例（替换类名）
java -cp target/classes zxf.java.threadpool.ThreadPoolExecutorParamsTests
```

各示例均为独立 `main` 程序，互不依赖，可单独运行观察输出。

---

## 示例清单

> 全部位于 `src/main/java/zxf/java/threadpool/`。

| 类名 | 主题 | 关键点 |
|---|---|---|
| `ExecutorTests` | `Executor` 顶层接口 | `newSingleThreadExecutor` + `execute`，最小接口形态 |
| `ExecutorServiceTests` | `ExecutorService` 接口 | `execute`/`submit`/`invokeAny`/`invokeAll`/`shutdown` |
| `ThreadPoolExecutorTests` | `Executors` 工厂方法 | `newFixedThreadPool` vs `newCachedThreadPool`，`getPoolSize`/`getQueue` |
| `ScheduledThreadPoolExecutorTests` | `ScheduledExecutorService` | `schedule`/`scheduleAtFixedRate`/`scheduleWithFixedDelay` |
| `ThreadPoolExecutorParamsTests` | **7 大构造参数 + 扩容流程** | 手动构造，可视化 core→queue→max→reject |
| `RejectedExecutionHandlerTests` | **4 种拒绝策略** | Abort / CallerRuns / Discard / DiscardOldest |
| `ForkJoinPoolTests` | ForkJoin 分治 + work-stealing | `RecursiveTask` 递归求和 |
| `SubmitVsExecuteExceptionTests` | **异常处理差异** | execute→UncaughtExceptionHandler；submit→Future 封装 |
| `ScheduledTaskExceptionTrapTests` | 周期任务异常陷阱 | 抛异常会导致后续周期静默停止 |
| `ThreadPoolMonitorHookTests` | 生命周期监控钩子 | `beforeExecute`/`afterExecute`/`terminated` |
| `VirtualThreadTests` | **JDK 21 虚拟线程** | 虚拟线程 vs 平台线程池吞吐对比 |
| `CompletableFutureTests` | 异步编排 | `thenApply`/`thenCompose`/`allOf`/`anyOf`/`exceptionally` |
| `ThreadPoolUtils` | 共享工具 | `sleep` / `shutdown` / `namedThreadFactory` |

---

## 线程池知识体系

```
Executor (顶层接口, 只有 execute)
  └─ ExecutorService (submit / invokeAny / invokeAll / shutdown)
       └─ ScheduledExecutorService (schedule / scheduleAtFixedRate / ...)
            └─ ScheduledThreadPoolExecutor
       └─ ThreadPoolExecutor  ← 绝大多数线程池的真正实现
       └─ ForkJoinPool         ← 分治 + work-stealing

Executors (工具类) → 提供各种预配置线程池工厂方法
CompletableFuture  → 异步任务编排（独立于线程池接口，但常配合使用）
```

> 注：`ScheduledThreadPoolExecutor` 实现 `ScheduledExecutorService` 的同时继承自 `ThreadPoolExecutor`，复用其线程管理能力；上图按接口维度组织。

### Executors 工厂方法对照

| 方法 | 特点 | 风险 |
|---|---|---|
| `newFixedThreadPool(n)` | 固定线程数，**无界队列** | 队列堆积 OOM |
| `newCachedThreadPool()` | 0 核心线程，**无界最大线程数** | 任务过多时线程数爆炸 |
| `newSingleThreadExecutor()` | 单线程 + 无界队列 | 队列堆积 OOM |
| `newScheduledThreadPool(n)` | 定时/周期任务 | 需自定义队列容量 |
| `newWorkStealingPool()` | 基于 `ForkJoinPool`，并行度=CPU 核数 | — |
| `newVirtualThreadPerTaskExecutor()` ⭐21 | 每任务一虚拟线程 | — |

> ⚠️ 阿里巴巴 Java 开发手册明确**禁止使用 `Executors` 创建线程池**，要求通过 `new ThreadPoolExecutor(...)` 显式指定参数，原因即上表的 OOM 风险。

---

## ThreadPoolExecutor 工作原理

### 7 大构造参数

```java
new ThreadPoolExecutor(
    int corePoolSize,                      // 核心线程数
    int maximumPoolSize,                   // 最大线程数
    long keepAliveTime, TimeUnit unit,     // 非核心线程空闲存活时间
    BlockingQueue<Runnable> workQueue,     // 任务队列
    ThreadFactory threadFactory,           // 线程工厂（命名、守护、异常处理器）
    RejectedExecutionHandler handler);     // 拒绝策略
```

### 任务提交后的执行流程（⭐ 核心）

```
提交任务
   │
   ├─ 当前线程数 < corePoolSize ?  → 创建新核心线程执行
   │
   ├─ workQueue 未满 ?             → 任务入队等待
   │
   ├─ 当前线程数 < maximumPoolSize?→ 创建非核心线程执行
   │
   └─ 触发拒绝策略                  → Abort / CallerRuns / Discard / DiscardOldest
```

> 这条流程是理解线程池行为的钥匙。运行 `ThreadPoolExecutorParamsTests` 可看到每一步的 `poolSize`/`queueSize` 变化。

### 4 种拒绝策略

| 策略 | 行为 |
|---|---|
| `AbortPolicy`（默认） | 抛 `RejectedExecutionException` |
| `CallerRunsPolicy` | 由提交任务的线程自己执行（背压降速） |
| `DiscardPolicy` | 静默丢弃新任务（危险，无提示） |
| `DiscardOldestPolicy` | 丢弃队列最老任务，重试提交新任务 |

### 常用监控方法

| 方法 | 含义 |
|---|---|
| `getPoolSize()` | 当前线程数 |
| `getActiveCount()` | 正在执行任务的线程数 |
| `getQueue().size()` | 队列堆积任务数 |
| `getLargestPoolSize()` | 历史峰值线程数 |
| `getCompletedTaskCount()` | 已完成任务总数 |
| `getTaskCount()` | 已接收任务总数 |

---

## 最佳实践

### 1. 手动构造线程池，禁止用 Executors

```java
// ❌ 禁止：无界队列，易 OOM
ExecutorService bad = Executors.newFixedThreadPool(10);

// ✅ 推荐：显式参数，有界队列
ThreadPoolExecutor good = new ThreadPoolExecutor(
        core, max, 60L, TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(1000),
        new ThreadFactoryBuilder().setNameFormat("biz-pool-%d").build(), // 线程命名（来自 Guava；也可用本项目 ThreadPoolUtils.namedThreadFactory）
        new ThreadPoolExecutor.CallerRunsPolicy());                       // 明确拒绝策略
```

### 2. 给线程命名

自定义 `ThreadFactory` 设置有意义的线程名（如 `order-pool-3`），排查问题时能立刻定位来源。本项目 `ThreadPoolUtils.namedThreadFactory(prefix)` 即此目的。

### 3. 合理设置线程数

| 任务类型 | 推荐线程数 |
|---|---|
| **CPU 密集型** | `N + 1`（N = CPU 核数） |
| **IO 密集型** | `2N` 或更多，或直接用虚拟线程 |

```java
int cpuBound = Runtime.getRuntime().availableProcessors() + 1;
int ioBound  = Runtime.getRuntime().availableProcessors() * 2;
```

### 4. 必须显式关闭

线程池默认创建**非守护线程**，不关闭会导致 JVM 无法退出。推荐模板：

```java
pool.shutdown();
try {
    if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
        pool.shutdownNow();
    }
} catch (InterruptedException e) {
    pool.shutdownNow();
    Thread.currentThread().interrupt();  // 恢复中断标志
}
```

### 5. 处理任务异常

- 用 `submit`：**必须**检查 `Future`（`get()` 或配合 `CompletableFuture.exceptionally`），否则异常被静默吞掉。
- 用 `execute`：通过 `ThreadFactory` 设置 `UncaughtExceptionHandler` 捕获。
- 周期任务：任务体内部 `try/catch` 所有异常，绝不让异常外泄中断周期。

详见 `SubmitVsExecuteExceptionTests` 与 `ScheduledTaskExceptionTrapTests`。

### 6. 监控线程池

继承 `ThreadPoolExecutor`，在 `beforeExecute` / `afterExecute` / `terminated` 中埋点，将线程池负载、任务耗时、失败率上报到 Prometheus / Micrometer 等监控系统。详见 `ThreadPoolMonitorHookTests`。

### 7. 不同业务隔离线程池

避免任务之间相互影响（例如慢任务拖垮快任务），按业务或重要性拆分独立线程池。

---

## JDK 21 虚拟线程

虚拟线程（[JEP 444](https://openjdk.org/jeps/444)）是 JDK 21 的核心特性：

- **轻量级**：由 JVM 调度在少量载体线程上，单 JVM 可创建数百万个。
- **适合 IO 密集**：阻塞时不占用 OS 线程，吞吐远高于平台线程池。
- **编程模型简单**：直接用同步阻塞代码写高并发，无需回调/响应式。

```java
// 每个任务一个虚拟线程
try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
    for (int i = 0; i < 10_000; i++) {
        executor.submit(() -> {
            // 阻塞 IO（HTTP、DB、文件）→ 虚拟线程挂起，释放载体线程
            return doIoWork();
        });
    }
} // try-with-resources 自动等待全部任务完成并关闭
```

> 运行 `VirtualThreadTests` 可看到 10000 个含 IO 等待的任务，虚拟线程比 200 平台线程池快数倍（实测 554ms vs 173ms，约 3 倍，具体倍数取决于机器）。
>
> **注意**：虚拟线程不适合 CPU 密集型任务（应继续用平台线程池或 ForkJoinPool）；不要池化虚拟线程（直接创建即可）。

### 虚拟线程遇冷的四个问题

虚拟线程虽在 JDK 21 转正，生产采用却普遍谨慎，主要卡在以下四点：

#### 1. Pinning（钉住）问题

JDK 21 中，虚拟线程在 `synchronized` 块/方法内发生阻塞（I/O、锁等待）时**无法从载体线程卸载**，被"钉"在载体线程上。载体线程默认只有 ≈ CPU 核数个，一旦大量虚拟线程被钉住，并行度骤降，"百万并发"名存实亡。

- **隐蔽**：第三方库内部一行 `synchronized` 就可能触发——MySQL Connector/J（[bug #109346](https://bugs.mysql.com/bug.php?id=109346)）、Snowflake JDBC、早期 Logback 均中过招。
- **检测**：`-Djdk.tracePinnedThreads=full`，或 JFR 事件 `jdk.VirtualThreadPinned`。
- **现状**：[JEP 491](https://openjdk.org/jeps/491)（JDK 24，2025-03）已修复——`synchronized` 内阻塞不再钉住载体线程（仅剩 native 栈帧持锁等少数场景仍可 pin）。但 JDK 21 及以下的存量生产环境无法享受该修复，只能把热点路径的 `synchronized` 改为 `ReentrantLock` 规避。

#### 2. ThreadLocal 内存陷阱

ThreadLocal 的生命周期与线程绑定，虚拟线程数量巨大，若用它缓存重量级对象（大缓冲区、`SimpleDateFormat`、连接缓存），百万并发 ≈ **百万份副本同时存活**，内存与 GC 压力剧增。[JEP 444](https://openjdk.org/jeps/444) 官方明确警告此用法。

官方替代品 `ScopedValue`（不可变、作用域绑定、自动清理）在 JDK 21 仅是**预览 API**（[JEP 446](https://openjdk.org/jeps/446)），直到 **JDK 25** 才转正（[JEP 506](https://openjdk.org/jeps/506)）；且迁移需把"随取随用"的 ThreadLocal 改写为"作用域包裹"的结构化代码，对存量系统改造成本不小。

> 对策：虚拟线程中不要用 ThreadLocal 缓存重量级对象；必须用时用完显式 `remove()`。

#### 3. 框架与中间件适配参差不齐

| 组件 | 情况 |
|---|---|
| JDBC 驱动 | 阻塞式模型可跑在虚拟线程上，但旧版本内部 `synchronized` 会导致 pinning，需升级到已修复版本 |
| Redis 客户端 | Jedis 5.x 起将内部 `synchronized` 换为锁；Lettuce 基于 Netty 本身异步，从虚拟线程获益有限 |
| 日志框架 | 早期 Logback 的 `synchronized` 写日志会 pin（后改为 ReentrantLock）；Log4j2 高并发下也暴露过问题 |
| Spring Boot | 3.2+ 支持一行开启 `spring.threads.virtual.enabled=true`，但升级 JDK + 审计全部依赖仍是工程量 |

**排查体验痛点**：虚拟线程**默认无名**——`getName()` 返回空字符串（平台线程至少有 `Thread-0`），日志与线程 dump 里满是空名线程，无法定位任务来源（Jetty 为此专门改了默认行为）。命名方式：

```java
Thread.ofVirtual().name("biz-", 0).start(task);                                    // biz-0, biz-1, ...
Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("biz-", 0).factory());  // 命名的虚拟线程执行器
```

#### 4. CPU 密集型场景无效

虚拟线程的全部收益来自"阻塞时挂起、让出载体线程"。CPU 密集任务（图片压缩、加解密、ETL 计算）不阻塞就不会卸载，照常占满 CPU 核，还要额外付出 mount/unmount 调度开销，实测往往更慢。JEP 444 原话：**"Virtual threads are not faster threads"**。

> CPU 密集任务继续用平台线程池（`N + 1`）或 `ForkJoinPool`（见 `ForkJoinPoolTests`），虚拟线程只服务 IO 密集场景。

---

## 常见陷阱

| 陷阱 | 后果 | 对策 |
|---|---|---|
| 不关闭线程池 | JVM 无法退出（进程挂起） | 显式 `shutdown` |
| 用 `Executors` 工厂 | 无界队列/线程易 OOM | 手动构造 `ThreadPoolExecutor` |
| `submit` 后不检查 Future | 异常静默丢失 | 检查 Future 或用 CompletableFuture |
| 周期任务抛未捕获异常 | 后续周期被静默取消 | 任务体 `try/catch` 全部异常 |
| `DiscardPolicy` 默认静默 | 任务无声丢失 | 至少用 `CallerRunsPolicy` 或加日志 |
| 把虚拟线程池化 | 失去轻量优势 | 每任务创建即用 |
| 虚拟线程在 `synchronized` 内阻塞 I/O | Pinning，并行度骤降 | JDK 24 已修复；JDK 21 用 `ReentrantLock` 规避 |
| 虚拟线程 ThreadLocal 缓存大对象 | 百万并发 = 百万副本，内存/GC 压力 | 不缓存重量级对象，用完 `remove()` |
| 虚拟线程默认无名 | 日志/线程 dump 显示空线程名，难定位 | `Thread.ofVirtual().name("prefix-", 0)` |

---

## 参考资料

- [JEP 444: Virtual Threads](https://openjdk.org/jeps/444)
- [JEP 491: Synchronize Virtual Threads without Pinning](https://openjdk.org/jeps/491)（JDK 24 修复 pinning）
- [JEP 506: Scoped Values](https://openjdk.org/jeps/506)（JDK 25 转正）
- [Java 并发编程实战](https://jcip.net/)
- [阿里巴巴 Java 开发手册](https://github.com/alibaba/p3c)
- JDK `java.util.concurrent` 包文档
