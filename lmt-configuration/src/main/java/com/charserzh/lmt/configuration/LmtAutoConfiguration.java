package com.charserzh.lmt.configuration;


import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.charserzh.lmt.core.annotation.EnableLMT;
import com.charserzh.lmt.core.config.LTBeanImporter;
import com.charserzh.lmt.core.config.LTCallbackMethodInterceptor;
import com.charserzh.lmt.core.config.LmtProperties;
import com.charserzh.lmt.core.config.job.LmtTask;
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

import javax.annotation.Resource;

@Configuration
@Import({LTBeanImporter.class})
@EnableLMT({"${lmt.base-packages}"})
@ConditionalOnProperty(name = {"lmt.enabled"}, havingValue = "true", matchIfMissing = false)
@MapperScan({"com.charserzh.lmt.core.repository.dao"})
@EnableConfigurationProperties({LmtProperties.class})
@AutoConfigureAfter({MybatisPlusAutoConfiguration.class, XXLJobAutoConfiguration.class})
public class LmtAutoConfiguration {

    @Resource
    private StatusTransactionRecordMapper statusTransactionRecordMapper;

    @Bean
    public StatusTransactionRecordRepository statusTransactionRecordRepository() {
        System.out.println("auto config in lmtAuto statusTransaction");
        return new StatusTransactionRecordRepositoryImpl(this.statusTransactionRecordMapper);
    }

    @Bean
    public LmtTask lmtTask(StatusTransactionRecordRepository statusTransactionRecordRepository) {
        return new LmtTask(statusTransactionRecordRepository);
    }

    @Bean
    public LTCallbackMethodInterceptor ltCallbackMethodInterceptor() {
        return new LTCallbackMethodInterceptor();
    }
}
