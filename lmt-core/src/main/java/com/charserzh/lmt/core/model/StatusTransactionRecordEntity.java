package com.charserzh.lmt.core.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * 本地消息事务表实体
 *
 * @author charserzh
 * @since 2023/11/15
 */
@NoArgsConstructor
@Data
public class StatusTransactionRecordEntity {

    /**
     * 自增主键
     */
    private Long id;

    /**
     * 执行状态 0.待执行 1.执行成功 2.执行失败
     */
    private Integer execStatus;

    /**
     * 首次执行时间
     */
    private Long firstExecTime;

    /**
     * 业务场景code
     */
    @NonNull
    private String bizSceneCode;

    /**
     * 业务场景描述
     */
    @NonNull
    private String bizScene;

    /**
     * 执行次数
     */
    private Integer execTimes;

    /**
     * 重试次数
     */
    private Integer retryTimes;

    /**
     * 业务ID,幂等键
     */
    @NonNull
    private String bizId;

    /**
     * 业务内容
     */
    @NonNull
    private String bizContent;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 逻辑删除，执行成功之后，会将此记录删除 1.已删除 0.未删除
     */
    private Integer isDelete;
}
