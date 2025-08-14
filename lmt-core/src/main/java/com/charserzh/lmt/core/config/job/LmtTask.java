package com.charserzh.lmt.core.config.job;

/**
 * 定时任务调度接口
 * @author zhanghao
 */
public interface LmtTask {

    LmtTaskResult execute(LmtTaskParam taskParam);
}
