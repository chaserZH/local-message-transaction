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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.io.File;

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
    @ConditionalOnMissingBean(DefaultLmtJobHandler.class)
    public DefaultLmtJobHandler defaultLmtJobHandler(LmtTaskUnified lmtTaskUnified) {
        return new DefaultLmtJobHandler(lmtTaskUnified);
    }

    @Component
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
