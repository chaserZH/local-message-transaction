package com.charserzh.lmt.core.repository.dao;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 本地消息事务记录表
 * @author zhanghao
 */
@Data
@TableName("t_status_transaction_record")
public class StatusTransactionRecordDO implements Serializable {

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 执行状态
     */
    private Integer execStatus;

    /**
     * 第一次执行时间
     */
    private Long firstExecTime;

    /**
     * 业务场景
     */
    private String bizScene;

    /**
     * 业务场景编码
     */
    private String bizSceneCode;

    /**
     * 执行次数
     */
    private Integer execTimes;

    /**
     * 重试次数
     */
    private Integer retryTimes;

    /**
     * 业务id
     */
    private String bizId;

    /**
     * 业务内容
     */
    private String bizContent;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 删除状态
     */
    private Integer isDelete;

    /**
     * 创建时间
     */
    private Long createTime;

    /**
     * 更新时间
     */
    private Long updateTime;
}
