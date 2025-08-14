# 本地消息事务表组件


## 一. 简介
### 1.1 背景
在日常开发中，我们会遇到一些需要异步处理的业务，或者无关核心业务的边界业务，边界业务的执行情况，不会影响核心业务结果，比如以下场景：
    * 订单保存之后（核心业务），发送一个push消息（边界业务）
    * 收到订单支付成功消息，本地订单状态更改(核心业务)，之后通知履约（边界业务）
以上边界业务执行失败或者异常，都不应该回滚核心业务，边界业务的失败可以通过重试来解决(ps : 开发者请关注下游系统幂等)。

### 1.2 场景回顾
* 核心业务：必须保证强一致性，比如订单保存、订单状态修改等。
* 边界业务：业务上的“附加操作”，不影响核心业务最终结果，比如发送推送结果通知、通知履约系统履约等。
* 边界业务失败不能回滚核心业务。
* 容错设计：边界业务失败可以重试或补偿。

### 1.3 解决思路总结
#### 1. 本地消息事务表
* 含义：将边界业务的任务信息（消息）存入本地数据库事务表（消息表）；
* 做法：
  * 现在同一个数据库事务里完成核心业务操作和边界任务记录（消息入库）；
  * 事务提交成功后，异步线程或定时任务读取消息表的信息，进行边界业务调用；
  * 边界业务执行成功，删除该消息记录；
  * 执行失败，可以重试或记录异常，防止丢失。
* 优点：核心业务和消息入库原子提交，避免了“发消息”与“核心业务提交”不一致的问题。
#### 2. 异步/即时任务触发
* 事务提交后，通过消息表触发异步处理，也可以用事件驱动、消息队列等方式完成边界业务处理。
#### 3.幂等设计
* 边界业务往往需要幂等处理，防止重复调用导致异常。

### 1.4解决边界业务失败的常见技术方案
| 方案           | 说明                                 |
|----------------|-------------------------------------|
| 本地消息表 + 消息投递 | 核心业务与消息表同事务，异步投递消息，消息消费端调用边界业务。 |
| 消息队列事务一致性 | 使用事务型消息中间件（RocketMQ事务消息、Kafka事务等）。 |
| 补偿机制         | 边界业务失败后，启动补偿任务或人工干预完成。           |
| 重试机制         | 消息投递失败后自动重试，直到成功或达到重试次数上限。       |

所以：将核心业务和 一起提交，事务提交之后，触发一个异步/即时 边界任务 ，协助完成整体的业务逻辑，如果边界任务
执行成功则删除任务记录，失败或者异常可以考虑重试。

## 二、本地消息事务表组件的解决思路
1. 将核心业务操作与边界业务的任务消息存入本地事务消息表操作绑定在一个事务里提交。
2. 异步/即时任务触发
   1. 检查是否存在事务，没有事务则直接异步触发边界业务回调
   2. 如果存在事务，添加一个事务同步器，重写afterCommit方法，然后异步触发边界业务。
3. aop拦截器拦截边界业务回调方法，如果边界业务回调方法执行成功，则修改本地消息事务表状态为终态。
4. 如果边界业务回调失败，则进行定时任务重试操作，超过重试次数不再重试。


## 三、核心技术
要想实现本地消息事务表，核心技术包含
### 3.1 Spring事务管理器和手动提交事务
核心类：TransactionSynchronizationManager 和 TransactionTemplate
```java
public abstract class AbstractTransactionCommand {
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    @Resource
    private Executor executor;

    public void triggerCallback(Supplier<Void> supplier) {
        Map<String, String> map = MDC.getCopyOfContextMap();
        log.info("current thread:{} with mdc:{}", Thread.currentThread(), map);
        // 判断当前是否存在事务
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            // 无事务，直接提交开始
            asyncRun(supplier, map);
            return;
        }
        // 有事务，添加一个事务同步器，重写afterCommit方法，
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
            @Override
            public void afterCommit() {
                // 直接提交
                asyncRun(supplier, map);
            }
        });
    }

    private void asyncRun(Supplier<Void> supplier, Map<String, String> map) {
        CompletableFuture
                .runAsync(() -> {
                    Optional.ofNullable(map).ifPresent(MDC::setContextMap);
                    supplier.get();
                }, executor)
                .exceptionally(throwable -> {
                    log.error("trigger callback error", throwable);
                    return null;
                })
                .whenComplete((unused, throwable) -> {
                    log.info("clear thread:{} mdc:{}", Thread.currentThread(), MDC.getCopyOfContextMap());
                    MDC.clear();
                });
    }
}
```
> 备注
> 1. 这个代码，我并没有放入到sdk包里，因为我认为这个代码，是和业务场景相关的，所以我没有放入到sdk包里。
> 2. 这个代码，需要创建异步线程池，涉及线程池的就关乎性能问题，所以让接入方自行创建。
> 3. 因为这个跟业务相关，接入方在执行具体业务可以直接继承这个类，然后实现具体的业务逻辑。
> 4. 这个代码跟spring手动提交事务，算是编程式事务，让用户手动提交事务，更能管理事务边界。
> 5. 这个代码前提是我们进行了手动事务管理，并重写了事务提交之后，执行我们注册的边界任务回调

#### 1. 支付成功履约为例
我们以以我们业务收到支付消息为例，首先我们做的是更改履约单状态，然后**通知履约**系统进行履约和**扣减库存**。
```java
public void payed(PayStatusEnum payStatusEnum) {
  TransactionTemplate transactionTemplate = SpringUtil.getBean(TransactionTemplate.class);
  transactionTemplate.execute(status -> {
    PayedStrategy payedStrategy = OrderFactory.pickPayedStrategy(productValue.
    getProductType());
    payedStrategy.payed(this, payStatusEnum);
    //
    triggerCallback(() -> {
      publishEvent(new PayedEvent(this));
      return null;
    });
    return null;
  });
}
```
我们以收到支付消息为例，首先我们通过transactionTemplate.execute() 手动提交了事务,（PS:手动提交事务，方便我们精准的事
务控制），重点关注payedStrategy.payed 这个方法.
```java
public void payed(OrderEntity entity, PayStatusEnum payStatusEnum) {
  if (payStatusEnum == PayStatusEnum.SUCCESS) {//
    AssertUtil.that(Objects.nonNull(entity.getPayInfoValue()), OrderErrorCode.PARAM_ERROR);
    entity.setOrderStatus(OrderStatusEnum.WAIT_FULFILLMENT);
    orderRepository.updatePayInfo(entity);
    // -
    StatusTransactionRecordEntity fulfillmentRecord = statusTransactionRecordRepository.save(
      new StatusTransactionRecordEntity(
      BizSceneEnum.NOTIFY_FULFILLMENT.getSceneCode(),
      BizSceneEnum.NOTIFY_FULFILLMENT.getDescription(),
      entity.getOrderTrackingIdValue().getOrderNo(),
      JSONUtil.toJsonStr(entity)
      )
    );
    // -
    StatusTransactionRecordEntity reduceStockRecord = statusTransactionRecordRepository.save(
      new StatusTransactionRecordEntity(
      BizSceneEnum.NOTIFY_REDUCE_STOCK.getSceneCode(),
      BizSceneEnum.NOTIFY_REDUCE_STOCK.getDescription(),
      entity.getOrderTrackingIdValue().getOrderNo(),
      JSONUtil.toJsonStr(entity)
      )
    );
    // -
    entity.triggerCallback(() -> {
      notifyFulfillmentCallback.callback(fulfillmentRecord);
    return null;
    });
    // -
    entity.triggerCallback(() -> {
      notifyReduceStockCallback.callback(reduceStockRecord);
    return null;
    });
    return;
  }
  log.warn("receive order payed,but pay status is not success ,ignore,{}", entity.
  getOrderTrackingIdValue().getOrderNo());
}
```
在支付方法里面，我们
1. 先进行 【订单状态更改】（核心业务），之后将【 通知履约】和**【通知扣减库存】** 当做任务写进本地消息事务表，之后完成事务提交
2. 事务提交之后，触发回调，回调的业务分别为，发起RPC调用，通知第三方履约和扣减库存

顺便看下我们是如何写回调业务的
```java
@Slf4j
@LMT(bizSceneCode = BizSceneEnum.CodeConstant.NOTIFY_FULFILLMENT_CODE)
public class NotifyFulfillmentCallback implements LTCallback {
  @Override
  public CallbackResultValue callback(StatusTransactionRecordEntity entity) {
    String bizContent = entity.getBizContent();
    OrderEntity orderEntity = JSONUtil.toBean(bizContent, OrderEntity.class);
    //
    try {
      NotifyFulfillmentStrategy notifyFulfillmentStrategy = OrderFactory.
      pickNotifyFulfillmentStrategy(orderEntity.getProductValue().getProductType());
      AssertUtil.that(Objects.nonNull(notifyFulfillmentStrategy), OrderErrorCode.
      PARAM_ERROR);
      notifyFulfillmentStrategy.notifyFulfillment(entity); // RPC
      return CallbackResultValue.builder().result(true).message(null).build();
    } catch (Exception e) {
      log.error("fulfillmentRepository.notifyFulfillment error", e);
      return CallbackResultValue.builder().result(false).message(e.getMessage()).build();
    }
  }
}
```
我们实现了LTCallback接口，并重写了callback方法，在内部，我们通过RPC 发起远程调用，并根据结果返回result=true还是false 而本地消息表背后的定时任务程序会自动帮助我们进行重试.

### 3.2 客户端接入回调接口
客户端接入这个sdk，必须自定义回调类，实现LTCallback接口，在回调接口里面写自己的业务逻辑,并自行返回执行结果是true还是false，false的时
候可以携带错误信息，告知程序执行结果，方便后续进行记录的状态更新，已经定时任务的执行。
```java
public interface LTCallback {
    /**
     * 回调方法
     *
     * @param paramStatusTransactionRecordEntity 本地消息事务记录实体
     * @return 回调结果
     */
    CallbackResultValue callback(StatusTransactionRecordEntity paramStatusTransactionRecordEntity);
}

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface LMT {

    /**
     * 场景码
     * @return 场景码
     */
    String bizSceneCode();
}
```

### 3.3 本地消息事务表重试机制
1. 我们sdk提供默认的xxl-job调度机制
2. 提供本地消息事务表自定义接口允许接入方自行使用job调度中间件(比如自研的调度中心)，然后调用我们的重试接口即可。
#### 1.提供默认的xxl-job调度
```java
/**
 * 只有配置了xxl-job配置项才会加载
 * @author zhanghao
 */
@Configuration
@ConditionalOnProperty(prefix = "xxl.job", name = "admin.addresses")
public class XXLJobAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(XXLJobAutoConfiguration.class);

    @Value("${xxl.job.admin.addresses}")
    private String adminAddresses;

    @Value("${xxl.job.executor.appname}")
    private String appName;

    @Value("${xxl.job.executor.port}")
    private int port;

    @Value("${xxl.job.accessToken}")
    private String accessToken;

    @Value("${xxl.job.executor.logpath}")
    private String logPath;

    @Value("${xxl.job.executor.logretentiondays}")
    private int logRetentionDays;

    @Bean
    @ConditionalOnMissingBean
    public XxlJobSpringExecutor xxlJobExecutor() {
        log.info("XXLJobAutoConfiguration loaded: XxlJobSpringExecutor init");
        XxlJobSpringExecutor executor = new XxlJobSpringExecutor();
        executor.setAdminAddresses(adminAddresses);
        executor.setAppname(appName);
        executor.setPort(port);
        executor.setAccessToken(accessToken);
        executor.setLogPath(logPath);
        executor.setLogRetentionDays(logRetentionDays);

        // 自动创建日志目录
        File logDir = new File(logPath);
        if (!logDir.exists()) {
            logDir.mkdirs();
        }
        return executor;
    }

    @Bean
    @ConditionalOnMissingBean(name = "lmtUnifiedJobHandler")
    public DefaultLmtJobHandler defaultLmtJobHandler(LmtTaskUnified lmtTaskUnified) {
        return new DefaultLmtJobHandler(lmtTaskUnified);
    }

    // 注意，这里没有 @Component
    public static class DefaultLmtJobHandler {

        private final LmtTaskUnified lmtTaskUnified;

        public DefaultLmtJobHandler(LmtTaskUnified lmtTaskUnified) {
            this.lmtTaskUnified = lmtTaskUnified;
        }

        @XxlJob("lmtUnifiedJobHandler")
        public ReturnT<String> execute(String jobParam) {
            long start = System.currentTimeMillis();
            log.info("Lmt task start at: {}", start);
            try {
                LmtTaskParam param = JSONUtil.toBean(jobParam, LmtTaskParam.class);
                LmtTaskResult result = lmtTaskUnified.execute(param);
                log.info("xxl-job execute, result: {}", result);
            } catch (Exception e) {
                log.error("xxl job transaction callback exec error", e);
                return ReturnT.FAIL;
            }
            long end = System.currentTimeMillis();
            log.info("Lmt task end at: {}, cost: {} ms", end, end - start);
            return ReturnT.SUCCESS;
        }
    }
}

```
* 如果接入方想使用我们的默认xxl-job调度中心来做本地消息事务重试，必须要配置xxl-job相关配置，才可以加载xxl-job调度中心进行重试，否则不加载。

#### 2. 自定义job调度中心
接入方如果想自定义job调度中心，只需要依赖引入的LmtTask接口接口
```java
public interface LmtTask {
    LmtTaskResult execute(LmtTaskParam taskParam);
}
```

### 3.4 回调接口aop拦截器
```java
/**
 * AOP 拦截器
 * 1. 拦截本地消息事务的回调方法
 * 2. 回调方法执行完成后，更新事务状态
 */
public class LTCallbackMethodInterceptor implements MethodInterceptor {

    private static final Logger log = LoggerFactory.getLogger(LTCallbackMethodInterceptor.class);

    private final StatusTransactionRecordRepository repository;

    public LTCallbackMethodInterceptor(StatusTransactionRecordRepository repository) {
        this.repository = repository;
    }


    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        log.info("local message transaction callback: {}, method: {}, args: {}", invocation.getThis(), invocation.getMethod().getName(), invocation.getArguments());

        if ("callback".equals(invocation.getMethod().getName())) {
            StatusTransactionRecordEntity entity = (StatusTransactionRecordEntity) invocation.getArguments()[0];
            try {
                if (TransactionExecStatusEnum.codeOf(entity.getExecStatus()) == TransactionExecStatusEnum.SUCCESS) {
                    log.warn("transaction record already success, ignore: {}", entity);
                    return null;
                }
                CallbackResultValue result = (CallbackResultValue) invocation.proceed();
                if (result != null && result.isResult()) {
                    entity.setExecStatus(1);
                    entity.setIsDelete(1);
                } else if (result != null) {
                    entity.setExecStatus(2);
                    entity.setErrorMessage(result.getMessage());
                }
                entity.setExecTimes((entity.getExecTimes() == null ? 0 : entity.getExecTimes()) + 1);
                repository.update(entity);
            } catch (Exception e) {
                log.error("local message transaction callback error", e);
                entity.setErrorMessage(e.getMessage());
                entity.setExecStatus(0);
                entity.setExecTimes((entity.getExecTimes() == null ? 0 : entity.getExecTimes()) + 1);
                repository.update(entity);
            }
        }
        return invocation.proceed();
    }
}

// com.charserzh.lmt.core.config.LTCallbackBeanPostProcessor.java
@Override
protected Object[] getAdvicesAndAdvisorsForBean(Class<?> beanClass, String beanName, TargetSource customTargetSource) throws BeansException {
    if (com.charserzh.lmt.core.callback.LTCallback.class.isAssignableFrom(beanClass)){
        return new Object[] { this.applicationContext.getBean(LTCallbackMethodInterceptor.class) };
    }
    return DO_NOT_PROXY;
}

```
* 当用户实现回调方法，绑定回调方法的bean，会被aop拦截，拦截器会更新本地消息事务状态


## 4. 最佳实践
### 4.1 引入starter包
```
<dependency>
    <groupId>com.chaserzh</groupId>
    <artifactId>lmt-configuration</artifactId>
    <version>1.0.0</version>
</dependency>
```
只需要引入starter包，就可以使用我们的本地消息事务功能

### 4.2 开启重试回调
开启重试回调组件，需要在配置文件配置
```
lmt:
  enabled: true
  base-packages: #             
```
其中base-packages可以不配置，默认是扫描启动类appliation所在的包以及"com.charserzh.lmt"所在目录的包（代码固定了）

### 4.3 xxl-job调度中心(可选)
* 如果需要使用默认的xxl-job作为本地消息事务表调度中心，需要在配置文件配置
```
xxl:
  job:
    admin:
      addresses: http://localhost:8080/xxl-job-admin
    accessToken: 123456   # 保持空
    executor:
      appname: lmt-executor
      port: 9999
      logpath: /Users/zhanghao/logs/xxl-job/jobhandler
      logretentiondays: 30
```

## 五、其他说明
### 5.1 项目结构
```
lmt-starter (demo应用)
↑
lmt-configuration (自动配置)
↑
lmt-core (核心实现)
```
接入方只需要引入lmt-configuration即可拆箱即用。

### 5.2 项目的bean的加载顺序说明
doc目录下的 springautoconfiguration.md
* 其中springautoconfiguration.md文件，介绍了项目的bean的加载顺序，以及项目的bean的作用。

### 5.3 本地消息事务表的sql语句
doc目录下的lmt.sql

### 5.4.docker-compose
doc目录下的docker-compose.yml文件，介绍了项目的docker-compose配置，以及项目的docker-compose的作用。
docker-compose.yml可能与电脑系统有关，我这边是mac 的M4芯片，存在兼容性问题，最终兼容采用了上诉的compose.yml文件





