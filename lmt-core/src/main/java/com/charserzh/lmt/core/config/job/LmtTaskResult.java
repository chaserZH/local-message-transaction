package com.charserzh.lmt.core.config.job;

import java.io.Serializable;

/**
 * @author zhanghao
 */
public class LmtTaskResult implements Serializable {

    /**
     * 成功次数
     */
    private int successCount;

    /**
     * 失败次数
     */
    private int failCount;

    public LmtTaskResult(int successCount, int failCount) {
        this.successCount = successCount;
        this.failCount = failCount;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public int getFailCount() {
        return failCount;
    }

    @Override
    public String toString() {
        return "LmtTaskResult{" +
                "successCount=" + successCount +
                ", failCount=" + failCount +
                '}';
    }
}
