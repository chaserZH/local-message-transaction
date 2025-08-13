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
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


public class LmtTask implements ApplicationContextAware, InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(LmtTask.class);
    private static final Long DEFAULT_CURRENT = 1L;
    private static final Long DEFAULT_PAGE_SIZE = 100L;

    private Map<String, LTCallback> callbackMap;
    private final StatusTransactionRecordRepository repository;

    private ApplicationContext applicationContext;

    public LmtTask(
            StatusTransactionRecordRepository repository
    ) {
        this.repository = repository;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Map<String, LTCallback> callbacks = this.applicationContext.getBeansOfType(LTCallback.class);
        this.callbackMap = callbacks.values().stream().collect(Collectors.toMap(km -> ((LMT)AopUtils.getTargetClass(km).getAnnotation(LMT.class)).bizSceneCode(), Function.identity()));

    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @XxlJob("orderTimeoutCancelJobHandler")
    public ReturnT<String> execute(String jobParam) {
        long currentTimeMillis = System.currentTimeMillis();
        log.info("Lmt task start at:{}", currentTimeMillis);
        try {
            LmtTaskParam param = parseParam(jobParam);
            processRecords(param);
            currentTimeMillis = System.currentTimeMillis();
            log.info("Lmt task end at:{}", currentTimeMillis);
            return ReturnT.SUCCESS;
        } catch (Exception e) {
            log.error("xxl job transaction callback  exec error", e);
            return new ReturnT<>(500, e.getMessage());
        }
    }

    private void processRecords(LmtTaskParam lmtTaskParam) {

        try {
            String bizSceneCode = lmtTaskParam.getBizSceneCode();
            String bizId = lmtTaskParam.getBizId();
            Long current = Objects.isNull(lmtTaskParam.getCurrent()) ? DEFAULT_CURRENT : lmtTaskParam.getCurrent();
            Long pageSize = Objects.isNull(lmtTaskParam.getPageSize()) ? DEFAULT_PAGE_SIZE : lmtTaskParam.getPageSize();
            lmtTaskParam.setCurrent(current);
            lmtTaskParam.setPageSize(pageSize);
            if (StringUtils.hasText(bizSceneCode) || StringUtils.hasText(bizId)) {
                Page<StatusTransactionRecordEntity> page;
                log.info("prepare to schedule exec lmt task  with bizSceneCode:{},bizId:{}", bizSceneCode, bizId);
                LTCallback ltCallback = this.callbackMap.get(bizSceneCode);
                if (Objects.isNull(ltCallback)) {
                    log.error("cant find any callback with bizSceneCode:{}, check ur callback config", bizSceneCode);
                    return ;
                }
                do {
                    try {
                        page = this.repository.findByParam(lmtTaskParam);
                    } catch (Exception e) {
                        log.warn("query transaction record error", e);
                        break;
                    }
                    page.getRecords().forEach(ltCallback::callback);
                    lmtTaskParam.setCurrent(lmtTaskParam.getCurrent() + 1L);
                } while (page.hasNext());
            }
        } catch (Exception e) {
            log.error("xxl job transaction callback  exec error", e);
        }
    }


    private LmtTaskParam parseParam(String jobParam) {
        LmtTaskParam param = JSONUtil.toBean(jobParam, LmtTaskParam.class);
        param.setCurrent(Optional.ofNullable(param.getCurrent()).orElse(DEFAULT_CURRENT));
        param.setPageSize(Optional.ofNullable(param.getPageSize()).orElse(DEFAULT_PAGE_SIZE));
        return param;
    }
}
