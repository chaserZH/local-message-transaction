package com.charserzh.lmt.core.config;

import com.ctrip.framework.apollo.model.ConfigChangeEvent;
import com.ctrip.framework.apollo.spring.annotation.ApolloConfigChangeListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.context.scope.refresh.RefreshScope;
import org.springframework.stereotype.Component;

@Component
public class SpringBootApolloRefreshConfig {
    private static final Logger log = LoggerFactory.getLogger(SpringBootApolloRefreshConfig.class);

    private final LmtProperties lmtProperties;

    private final RefreshScope refreshScope;

    public SpringBootApolloRefreshConfig(LmtProperties lmtProperties, RefreshScope refreshScope) {
        this.lmtProperties = lmtProperties;
        this.refreshScope = refreshScope;
    }


    @ApolloConfigChangeListener(value = {"application", "application.yml"}, interestedKeyPrefixes = {"lmt"})
    public void onChange(ConfigChangeEvent changeEvent) {
        log.info("before refresh {}", this.lmtProperties.toString());
        boolean result = this.refreshScope.refresh("lmtProperties");
        log.info("after refresh {},result:{}", this.lmtProperties, result);
    }
}
