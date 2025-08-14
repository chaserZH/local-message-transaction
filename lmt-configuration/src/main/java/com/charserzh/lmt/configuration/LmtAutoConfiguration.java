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
@EnableLMT
@ConditionalOnProperty(name = "lmt.enabled", havingValue = "true", matchIfMissing = false)
@MapperScan({"com.charserzh.lmt.core.repository.dao"})
@AutoConfigureAfter({MybatisPlusAutoConfiguration.class})
public class LmtAutoConfiguration {

    @Resource
    private StatusTransactionRecordMapper statusTransactionRecordMapper;

    @Bean
    public StatusTransactionRecordRepository statusTransactionRecordRepository() {
        System.out.println("LmtAutoConfiguration loaded: StatusTransactionRecordRepository init");
        return new StatusTransactionRecordRepositoryImpl(this.statusTransactionRecordMapper);
    }

    @Bean
    public LmtTaskUnified lmtTaskUnified(StatusTransactionRecordRepository repository,
                                         ExecutorService lmtExecutorService) {
        System.out.println("LmtAutoConfiguration loaded: LmtTaskUnified init");
        return new LmtTaskUnified(repository, lmtExecutorService);
    }

    @Bean
    public LTCallbackMethodInterceptor ltCallbackMethodInterceptor(@Lazy StatusTransactionRecordRepository repository) {
        System.out.println("LmtAutoConfiguration loaded: LTCallbackMethodInterceptor init");
        return new LTCallbackMethodInterceptor(repository);
    }

    @PostConstruct
    public void init() {
        System.out.println("LmtAutoConfiguration initialized successfully");
    }
}
