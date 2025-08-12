package com.charserzh.lmt.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Data
@ConfigurationProperties(LmtProperties.PREFIX)
public class LmtProperties {

    public static final String PREFIX = "lmt";

    private boolean enabled;

    private String[] basePackages;
}


