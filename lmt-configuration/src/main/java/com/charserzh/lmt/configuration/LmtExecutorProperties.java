package com.charserzh.lmt.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;


@ConfigurationProperties(prefix = "lmt.executor")
public class LmtExecutorProperties {

    /** 核心线程数 */
    private int corePoolSize = 100;

    /** 最大线程数 */
    private int maxPoolSize = 100;

    /** 队列容量 */
    private int queueCapacity = 9900;

    /** 空闲线程存活时间(秒) */
    private long keepAliveSeconds = 10;

    // getters & setters
    public int getCorePoolSize() { return corePoolSize; }
    public void setCorePoolSize(int corePoolSize) { this.corePoolSize = corePoolSize; }

    public int getMaxPoolSize() { return maxPoolSize; }
    public void setMaxPoolSize(int maxPoolSize) { this.maxPoolSize = maxPoolSize; }

    public int getQueueCapacity() { return queueCapacity; }
    public void setQueueCapacity(int queueCapacity) { this.queueCapacity = queueCapacity; }

    public long getKeepAliveSeconds() { return keepAliveSeconds; }
    public void setKeepAliveSeconds(long keepAliveSeconds) { this.keepAliveSeconds = keepAliveSeconds; }
}
