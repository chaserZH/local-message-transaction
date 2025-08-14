package com.charserzh.lmt.core.config.job;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.charserzh.lmt.core.annotation.LMT;
import com.charserzh.lmt.core.callback.LTCallback;
import com.charserzh.lmt.core.model.CallbackResultValue;
import com.charserzh.lmt.core.model.StatusTransactionRecordEntity;
import com.charserzh.lmt.core.repository.StatusTransactionRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 统一任务
 * 1. 定时任务串行或者分片并行
 * {
 *   "bizSceneCode": "ORDER_PAY_TIMEOUT",
 *   "pageSize": 200,
 *   "current": 1,
 *   "tailNums": "0,1,2",
 *   "onlyFailed": true,
 *   "parallel": true
 * }
 *
 * @author zhanghao
 */
public class LmtTaskUnified implements LmtTask, ApplicationContextAware, InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(LmtTaskUnified.class);


    private static final Long DEFAULT_CURRENT = 1L;
    private static final Long DEFAULT_PAGE_SIZE = 100L;

    private final StatusTransactionRecordRepository repository;
    private Map<String, LTCallback> callbackMap = new HashMap<>();
    private ApplicationContext applicationContext;

    private final ExecutorService executorService;


    public LmtTaskUnified(StatusTransactionRecordRepository repository, ExecutorService executorService) {
        this.repository = repository;
        this.executorService = executorService;
    }

    @Override
    public void afterPropertiesSet() {
        Map<String, LTCallback> beans = applicationContext.getBeansOfType(LTCallback.class);
        this.callbackMap = beans.values().stream()
                .collect(Collectors.toMap(
                        km -> AopUtils.getTargetClass(km).getAnnotation(LMT.class).bizSceneCode(),
                        Function.identity(),
                        (existing, replacement) -> {
                            log.warn("Duplicate LTCallback found for bizSceneCode: {}", existing);
                            return existing;
                        }
                ));
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }


    @Override
    public LmtTaskResult execute(LmtTaskParam param) {
        long start = System.currentTimeMillis();
        log.info("LmtUnified task start, param: {}", param);

        param.setCurrent(Optional.ofNullable(param.getCurrent()).orElse(DEFAULT_CURRENT));
        param.setPageSize(Optional.ofNullable(param.getPageSize()).orElse(DEFAULT_PAGE_SIZE));

        int successCount = 0;
        int failCount = 0;
        int batchSize = 50;

        Page<StatusTransactionRecordEntity> page;
        do {
            page = repository.findByParam(param);

            List<StatusTransactionRecordEntity> records = Optional.ofNullable(page.getRecords())
                    .orElse(Collections.emptyList())
                    .stream()
                    .filter(r -> filterByTailNums(r.getId(), param.getTailNums()))
                    .filter(r -> !param.isOnlyFailed() || r.getExecStatus() != 1)
                    .collect(Collectors.toList());

            if (!records.isEmpty()) {
                if (Boolean.TRUE.equals(param.getParallel())) {
                    // 并行模式，分批处理
                    for (int i = 0; i < records.size(); i += batchSize) {
                        int end = Math.min(i + batchSize, records.size());
                        List<StatusTransactionRecordEntity> batch = records.subList(i, end);

                        CompletionService<LmtTaskResult> completionService =
                                new ExecutorCompletionService<>(executorService);

                        // 提交批次任务
                        batch.forEach(record -> completionService.submit(() -> processRecord(param, record)));

                        // 汇总批次结果
                        for (int j = 0; j < batch.size(); j++) {
                            try {
                                LmtTaskResult r = completionService.take().get();
                                successCount += r.getSuccessCount();
                                failCount += r.getFailCount();
                            } catch (Exception e) {
                                log.error("Error waiting for parallel task", e);
                                failCount++;
                            }
                        }
                    }
                } else {
                    // 同步模式，直接执行
                    for (StatusTransactionRecordEntity record : records) {
                        LmtTaskResult r = processRecord(param, record);
                        successCount += r.getSuccessCount();
                        failCount += r.getFailCount();
                    }
                }
            }

            param.setCurrent(param.getCurrent() + 1);
        } while (page.hasNext());

        long end = System.currentTimeMillis();
        log.info("LmtUnified task end, duration: {}ms, success: {}, fail: {}",
                (end - start), successCount, failCount);

        return new LmtTaskResult(successCount, failCount);
    }

    /**
     * 统一处理单条记录
     */
    private LmtTaskResult processRecord(LmtTaskParam param, StatusTransactionRecordEntity record) {
        try {
            LTCallback callback = getCallback(param, record);
            if (callback != null) {
                CallbackResultValue result = callback.callback(record);
                return (result != null && result.isResult()) ? new LmtTaskResult(1, 0) : new LmtTaskResult(0, 1);
            } else {
                return new LmtTaskResult(0, 1);
            }
        } catch (Exception e) {
            log.error("Processing record failed: {}", record.getId(), e);
            return new LmtTaskResult(0, 1);
        }
    }



    /**
     * 获取回调方法
     * @param param 任务参数
     * @param record 记录实体
     * @return 回调方法
     */
    private LTCallback getCallback(LmtTaskParam param, StatusTransactionRecordEntity record) {
        String sceneCode = StringUtils.hasText(param.getBizSceneCode())
                ? param.getBizSceneCode()
                : (StringUtils.hasText(record.getBizSceneCode()) ? record.getBizSceneCode() : null);

        if (StringUtils.hasText(sceneCode)) {
            LTCallback callback = callbackMap.get(sceneCode);
            if (callback == null) {
                log.warn("No callback found for bizSceneCode: {}", sceneCode);
            }
            return callback;
        }
        return null;
    }

    /**
     * 分片过滤
     * @param recordId 记录ID
     * @param tailNums 分片尾号
     * @return 是否通过
     */
    private boolean filterByTailNums(Long recordId, String tailNums) {
        if (!StringUtils.hasText(tailNums)) {
            return true;
        }
        long tail = recordId % 10;
        return Arrays.stream(tailNums.split(","))
                .map(String::trim)
                .mapToLong(Long::parseLong)
                .anyMatch(t -> t == tail);
    }
}
