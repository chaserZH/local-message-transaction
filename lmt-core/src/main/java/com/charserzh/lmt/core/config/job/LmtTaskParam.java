package com.charserzh.lmt.core.config.job;

import cn.hutool.core.lang.Pair;
import lombok.Data;

import java.io.Serializable;

/**
 * 定时任务参数
 * @author zhanghao
 */
@Data
public class LmtTaskParam implements Serializable {

    /**
     * 业务场景编码 - 可选
     */
    private String bizSceneCode;

    /**
     * 业务ID，可选，用于单条处理
     */
    private String bizId;

    /**
     * 分页参数：每页大小，默认100
     */
    private Long pageSize;

    /**
     * 分页参数：当前页，默认1
     */
    private Long current;

    /**
     * 分片尾号，逗号分隔，如 "0,1,2"，用于并行任务分片
     */
    private String tailNums;

    /**
     * 查询失败的
     */
    private boolean onlyFailed = true;

    /** 是否并行处理，true 并行，false 同步处理 */
    private Boolean parallel;
}
