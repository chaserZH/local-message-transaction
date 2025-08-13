# 自动装配说明

## 1. 装配入口
com.charserzh.lmt.configuration.LmtAutoConfiguration

## 2. 自动装配文件(spring.factories)
> lmt-configuration/src/main/resources/META-INF/spring.factories

org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
com.charserzh.lmt.configuration.LmtAutoConfiguration,\
com.charserzh.lmt.configuration.XXLJobAutoConfiguration,\
com.charserzh.lmt.configuration.LmtExecutorConfig

## 3. 自动装配类
LmtAutoConfiguration自动装配初始化顺序

Spring Boot 容器启动
┌──────────────────────────────┐
│   自动扫描 / spring.factories │
└─────────────┬────────────────┘
│
▼
┌──────────────────────────────┐
│ XXLJobAutoConfiguration       │
│  @Configuration               │
│  Bean: XxlJobSpringExecutor   │
└─────────────┬────────────────┘
│  （AutoConfigureAfter 保证顺序）
▼
┌──────────────────────────────┐
│ LmtExecutorConfig             │
│  @Configuration               │
│  Bean: lmtExecutorService     │
│  使用属性配置创建线程池       │
└─────────────┬────────────────┘
│
▼
┌──────────────────────────────┐
│ LmtAutoConfiguration          │
│  @Configuration               │
│  Bean: StatusTransactionRepo  │
│  Bean: LmtTaskUnified         │
│    - 注入 repository          │
│    - 注入 lmtExecutorService  │
│  Bean: LTCallbackMethodInterceptor │
└─────────────┬────────────────┘
│
▼
┌──────────────────────────────┐
│ LmtTaskUnified 执行定时任务   │
│  - 并行模式使用线程池         │
│  - 同步模式逐条处理           │
└──────────────────────────────┘

### 说明
1. XXLJobAutoConfiguration 先加载，保证 XxlJobSpringExecutor 可用。
2. LmtExecutorConfig 创建默认线程池（除非接入方自定义 Bean）。
3. LmtAutoConfiguration 注入核心 Bean：
   *  StatusTransactionRecordRepository
   *  LmtTaskUnified（注入线程池）
   *  LTCallbackMethodInterceptor

4. LmtTaskUnified 执行任务：
   * 并行模式使用注入线程池
   * 同步模式直接逐条执行回调

## 接入方使用线程池属性 vs 自定义 Bean 的决策流程图
```mermaid
Spring 容器启动
        │
        ▼
检查容器中是否存在 ExecutorService 类型的 Bean
        │
   ┌────┴────┐
   │         │
有自定义 Bean   没有自定义 Bean
   │         │
   ▼         ▼
使用接入方提供的线程池  创建默认线程池（LmtExecutorConfig）
   │         │
   │         ▼
   │   ThreadPoolExecutor(
   │       corePoolSize = lmt.executor.corePoolSize,
   │       maxPoolSize  = lmt.executor.maxPoolSize,
   │       keepAlive   = lmt.executor.keepAliveSeconds,
   │       queueCapacity = lmt.executor.queueCapacity,
   │       NamedThreadFactory("lmtExecutor")
   │   )
   │
   ▼
注入到 LmtTaskUnified
        │
        ▼
任务执行：
- 并行模式 → 使用注入线程池执行 LmtArrayTask
- 同步模式 → 逐条调用 callback
```

## SDK 自动装配 + 线程池决策流程图
展示 LmtTaskUnified、线程池和接入方配置之间的关系。
```mermaid
┌───────────────────────┐
│    接入方 Spring Boot  │
│   application.yml 或   │
│   自定义 ExecutorService │
└─────────┬─────────────┘
          │
          │ 1. 提供 ExecutorService Bean?
          ▼
┌─────────────────────────────┐
│       Spring 容器扫描        │
│  @EnableConfigurationProperties │
│  @Import(LTBeanImporter)      │
│  自动注册 LmtAutoConfiguration │
└─────────┬───────────────────┘
          │
          │ 2. 注入 ExecutorService
          ▼
┌─────────────────────────────┐
│   LmtExecutorConfig Bean     │
│  @ConditionalOnMissingBean   │
│  默认创建 ThreadPoolExecutor │
└─────────┬───────────────────┘
          │
          │ 3. 注入 LmtTaskUnified
          ▼
┌─────────────────────────────┐
│      LmtTaskUnified         │
│  - repository 注入           │
│  - ExecutorService 注入      │
│  - 处理任务，可并行或顺序    │
│    并行 -> 提交到 executorService │
└─────────┬───────────────────┘
          │
          │ 4. 执行 LmtArrayTask (批量处理)
          ▼
┌─────────────────────────────┐
│       LmtArrayTask          │
│ - 接收 ExecutorService      │
│ - 并行处理批量 StatusTransactionRecordEntity │
└─────────────────────────────┘
```
流程说明
1. 接入方提供 ExecutorService
   * 优先使用接入方自定义线程池（例如性能可控的 ThreadPoolExecutor）
2. 未提供线程池
   * SDK 会使用默认线程池（LmtExecutorConfig 创建）
3. LmtTaskUnified
   * 注入仓储、线程池
   * 根据 param.parallel=true 决定是否并行处理
4. 并行处理
   * 批量任务交给 LmtArrayTask 提交到线程池执行
5. 顺序处理
   * 直接在主线程处理每条记录


