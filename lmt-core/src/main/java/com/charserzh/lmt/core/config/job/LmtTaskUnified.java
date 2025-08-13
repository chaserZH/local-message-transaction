package com.charserzh.lmt.core.config.job;

import cn.hutool.core.thread.NamedThreadFactory;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.charserzh.lmt.core.annotation.LMT;
import com.charserzh.lmt.core.callback.LTCallback;
import com.charserzh.lmt.core.model.StatusTransactionRecordEntity;
import com.charserzh.lmt.core.repository.StatusTransactionRecordRepository;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.handler.annotation.XxlJob;
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
import java.util.stream.Stream;

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
public class LmtTaskUnified implements ApplicationContextAware, InitializingBean {

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

    @XxlJob("lmtUnifiedJobHandler")
    public ReturnT<String> execute(String jobParam) {
        long start = System.currentTimeMillis();
        log.info("LmtUnified task start at: {}, jobParam: {}", start, jobParam);

        try {
            LmtTaskParam param = parseParam(jobParam);
            processRecords(param);
            long end = System.currentTimeMillis();
            log.info("LmtUnified task end at: {}, duration: {}ms", end, (end - start));
            return ReturnT.SUCCESS;
        } catch (Exception e) {
            log.error("LmtUnified task execution error", e);
            return new ReturnT<>(500, e.getMessage());
        }
    }

    private LmtTaskParam parseParam(String jobParam) {
        LmtTaskParam param = JSONUtil.toBean(jobParam, LmtTaskParam.class);
        param.setCurrent(Optional.ofNullable(param.getCurrent()).orElse(DEFAULT_CURRENT));
        param.setPageSize(Optional.ofNullable(param.getPageSize()).orElse(DEFAULT_PAGE_SIZE));
        return param;
    }

    private void processRecords(LmtTaskParam param) {
        Page<StatusTransactionRecordEntity> page;
        do {
            page = repository.findByParam(param);
            List<StatusTransactionRecordEntity> records = page.getRecords();

            if (Boolean.TRUE.equals(param.getParallel())) {
                // 并行处理：按批次提交到线程池
                executorService.submit(new LmtArrayTask(records, getCallback(param, records)));
            } else {
                // 同步处理：直接逐条执行 callback
                records.stream()
                        .filter(record -> filterByTailNums(record.getId(), param.getTailNums()))
                        .filter(record -> !param.isOnlyFailed() || record.getExecStatus() != 1)
                        .forEach(record -> {
                            try {
                                getCallback(param, Collections.singletonList(record)).callback(record);
                            } catch (Exception e) {
                                log.error("Processing record failed: {}", record.getId(), e);
                            }
                        });
            }

            param.setCurrent(param.getCurrent() + 1);
        } while (page.hasNext());
    }

    private LTCallback getCallback(LmtTaskParam param, List<StatusTransactionRecordEntity> records) {
        String sceneCode = StringUtils.hasText(param.getBizSceneCode())
                ? param.getBizSceneCode()
                : (records.isEmpty() ? null : records.get(0).getBizSceneCode());
        LTCallback callback = callbackMap.get(sceneCode);
        if (callback == null) {
            log.warn("No callback found for bizSceneCode: {}", sceneCode);
        }
        return callback;
    }

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
