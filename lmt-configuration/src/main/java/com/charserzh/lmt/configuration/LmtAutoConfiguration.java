package com.charserzh.lmt.configuration;


import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.charserzh.lmt.core.annotation.EnableLMT;
import com.charserzh.lmt.core.config.LTBeanImporter;
import com.charserzh.lmt.core.config.LTCallbackMethodInterceptor;
import com.charserzh.lmt.core.config.LmtProperties;
import com.charserzh.lmt.core.config.job.LmtTaskUnified;
import com.charserzh.lmt.core.repository.StatusTransactionRecordRepository;
import com.charserzh.lmt.core.repository.dao.StatusTransactionRecordMapper;
import com.charserzh.lmt.core.repository.impl.StatusTransactionRecordRepositoryImpl;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.concurrent.ExecutorService;

@Configuration
@Import({LTBeanImporter.class})
@EnableConfigurationProperties({LmtProperties.class, LmtExecutorProperties.class})
@EnableLMT("${lmt.base-packages}")
@ConditionalOnProperty(name = "lmt.enabled", havingValue = "true", matchIfMissing = false)
@MapperScan({"com.charserzh.lmt.core.repository.dao"})
@AutoConfigureAfter({MybatisPlusAutoConfiguration.class, XXLJobAutoConfiguration.class})
public class LmtAutoConfiguration {

    @Resource
    private StatusTransactionRecordMapper statusTransactionRecordMapper;

    /**
     * 注入本地消息事务仓储
     */
    @Bean
    public StatusTransactionRecordRepository statusTransactionRecordRepository() {
        System.out.println("LmtAutoConfiguration loaded: StatusTransactionRecordRepository init");
        return new StatusTransactionRecordRepositoryImpl(this.statusTransactionRecordMapper);
    }

    /**
     * 注入 LmtTaskUnified
     * 自动注入线程池：
     * - 优先使用接入方自定义 lmtExecutorService Bean
     * - 如果不存在则使用默认线程池（LmtExecutorConfig 创建）
     */
    @Bean
    public LmtTaskUnified lmtTaskUnified(StatusTransactionRecordRepository repository,
                                         ExecutorService lmtExecutorService) {
        return new LmtTaskUnified(repository, lmtExecutorService);
    }

    /**
     * 注册 AOP 拦截器，用于 LTCallback 回调方法
     */
    @Bean
    public LTCallbackMethodInterceptor ltCallbackMethodInterceptor(@Lazy StatusTransactionRecordRepository repository) {
        return new LTCallbackMethodInterceptor(repository);
    }

    @PostConstruct
    public void init() {
        System.out.println("LmtAutoConfiguration initialized successfully");
    }
}
