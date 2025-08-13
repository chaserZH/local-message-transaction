package com.charserzh.lmt.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;



@ConfigurationProperties(LmtProperties.PREFIX)
public class LmtProperties {

    public static final String PREFIX = "lmt";

    private boolean enabled;

    private String[] basePackages;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String[] getBasePackages() {
        return basePackages;
    }

    public void setBasePackages(String[] basePackages) {
        this.basePackages = basePackages;
    }
}


