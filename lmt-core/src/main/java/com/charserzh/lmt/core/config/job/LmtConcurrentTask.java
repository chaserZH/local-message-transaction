package com.charserzh.lmt.core.config.job;

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
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;



import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.xxl.job.core.context.XxlJobHelper;

public class LmtConcurrentTask implements ApplicationContextAware, InitializingBean {
    private static final Logger log = LoggerFactory.getLogger(LmtConcurrentTask.class);
    private static final Long DEFAULT_CURRENT = 1L;
    private static final Long DEFAULT_PAGE_SIZE = 4000L;

    private final StatusTransactionRecordRepository statusTransactionRecordRepository;
    private Map<String, LTCallback> callbackMap = new HashMap<>();
    private ApplicationContext applicationContext;

    public LmtConcurrentTask(StatusTransactionRecordRepository statusTransactionRecordRepository) {
        this.statusTransactionRecordRepository = statusTransactionRecordRepository;
    }

    @Override
    public void afterPropertiesSet() {
        Map<String, LTCallback> callbacks = applicationContext.getBeansOfType(LTCallback.class);
        this.callbackMap = callbacks.values().stream()
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

    @XxlJob("lmtConcurrentRetryHandler")
    public ReturnT<String> execute() {
        // 1. 获取分片参数
        String jobParam = XxlJobHelper.getJobParam();
        int index = XxlJobHelper.getShardIndex();
        int total = XxlJobHelper.getShardTotal();

        log.info("Job[{}] started with param: {}, shard: {}/{}",
                XxlJobHelper.getJobId(), jobParam, index, total);

        try {
            LmtTaskParam param = parseParam(jobParam);
            processRecords(param, index, total);
            return ReturnT.SUCCESS;
        } catch (Exception e) {
            log.error("Job[{}] failed", XxlJobHelper.getJobId(), e);
            XxlJobHelper.log("Job failed: {}", e.getMessage());
            return new ReturnT<>(500, e.getMessage());
        }
    }

    private LmtTaskParam parseParam(String jobParam) {
        LmtTaskParam param = JSONUtil.toBean(jobParam, LmtTaskParam.class);
        param.setCurrent(Optional.ofNullable(param.getCurrent()).orElse(DEFAULT_CURRENT));
        param.setPageSize(Optional.ofNullable(param.getPageSize()).orElse(DEFAULT_PAGE_SIZE));
        return param;
    }

    private void processRecords(LmtTaskParam param, int shardIndex, int shardTotal) {
        if (!StringUtils.hasText(param.getBizSceneCode())) {
            log.warn("Empty bizSceneCode in param");
            return;
        }

        LTCallback callback = callbackMap.get(param.getBizSceneCode());
        if (callback == null) {
            log.error("No callback found for bizSceneCode: {}", param.getBizSceneCode());
            return;
        }

        Page<StatusTransactionRecordEntity> page;
        do {
            // MyBatis-Plus 分页查询
            page = statusTransactionRecordRepository.findByParam(param);

            // 获取记录列表
            List<StatusTransactionRecordEntity> records = filterRecords(
                    page.getRecords(),
                    shardIndex,
                    shardTotal,
                    param.getTailNums()
            );

            // 并行处理
            records.parallelStream().forEach(record -> {
                try {
                    callback.callback(record);
                } catch (Exception e) {
                    log.error("Process record failed: {}", record.getId(), e);
                }
            });

            // MyBatis-Plus 分页判断：检查当前页是否小于总页数
            param.setCurrent(param.getCurrent() + 1);
        } while (param.getCurrent() <= page.getPages());  // getPages() 获取总页数
    }

    private List<StatusTransactionRecordEntity> filterRecords(
            List<StatusTransactionRecordEntity> records,
            int shardIndex,
            int shardTotal,
            String tailNums) {
        return records.stream()
                .filter(record -> record.getId() % shardTotal == shardIndex)
                .filter(record -> filterByTailNum(record.getId(), tailNums))
                .collect(Collectors.toList());
    }

    private boolean filterByTailNum(Long recordId, String tailNums) {
        if (StringUtils.isEmpty(tailNums)) {
            return true;
        }
        long tail = recordId % 10;
        return Arrays.stream(tailNums.split(","))
                .map(Long::parseLong)
                .anyMatch(t -> t == tail);
    }
}