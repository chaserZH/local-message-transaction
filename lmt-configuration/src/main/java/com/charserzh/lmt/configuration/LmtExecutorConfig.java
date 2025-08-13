package com.charserzh.lmt.configuration;

import cn.hutool.core.thread.NamedThreadFactory;
import com.charserzh.lmt.core.config.job.LmtTaskUnified;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
public class LmtExecutorConfig {
    private static final Logger log = LoggerFactory.getLogger(LmtExecutorConfig.class);

    /**
     * 创建 SDK 默认线程池
     * 优先使用属性配置，保证性能安全。
     * 如果接入方提供自定义 Bean（lmtExecutorService）会被覆盖。
     */
    @Bean(name = "lmtExecutorService")
    @ConditionalOnMissingBean(name = "lmtExecutorService")
    public ExecutorService lmtExecutorService(LmtExecutorProperties properties) {
        log.info("Creating default LMT ExecutorService with corePoolSize={}, maxPoolSize={}, queueCapacity={}",
                properties.getCorePoolSize(), properties.getMaxPoolSize(), properties.getQueueCapacity());

        return new ThreadPoolExecutor(
                properties.getCorePoolSize(),
                properties.getMaxPoolSize(),
                properties.getKeepAliveSeconds(),
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(properties.getQueueCapacity()),
                new NamedThreadFactory("lmtExecutor", true),
                (r, executor) -> LoggerFactory.getLogger(LmtTaskUnified.class)
                        .error("lmtExecutorService rejectedExecution, task is {}", r)
        );
    }
}
