package com.charserzh.lmt.configuration;

import cn.hutool.json.JSONUtil;
import com.charserzh.lmt.core.config.job.LmtTaskParam;
import com.charserzh.lmt.core.config.job.LmtTaskResult;
import com.charserzh.lmt.core.config.job.LmtTaskUnified;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

/**
 * 提供默认的xxl-job加载器
 * 只有配置了xxl-job配置项才会加载
 * @author zhanghao
 */
@Component
@ConditionalOnBean(XxlJobSpringExecutor.class) // 只有 XXL-Job Bean 存在时才加载
public class DefaultLmtJobHandler {

    private static final Logger log = LoggerFactory.getLogger(DefaultLmtJobHandler.class);

    private final LmtTaskUnified lmtTaskUnified;

    public DefaultLmtJobHandler(LmtTaskUnified lmtTaskUnified) {
        this.lmtTaskUnified = lmtTaskUnified;
    }

    @XxlJob("lmtUnifiedJobHandler")
    public ReturnT<String> execute(String jobParam) {
        long currentTimeMillis = System.currentTimeMillis();
        log.info("Lmt task start at:{}", currentTimeMillis);
        try {
            LmtTaskParam param = JSONUtil.toBean(jobParam, LmtTaskParam.class);
            LmtTaskResult result = lmtTaskUnified.execute(param);
            log.info("xxl-job execute, result: {}", result);
        }catch (Exception e) {
            log.error("xxl job transaction callback  exec error", e);
        }
        long endTimeMillis = System.currentTimeMillis();
        log.info("Lmt task end at :{},cost:{}", endTimeMillis,endTimeMillis - currentTimeMillis);
        return ReturnT.SUCCESS;
    }
}
