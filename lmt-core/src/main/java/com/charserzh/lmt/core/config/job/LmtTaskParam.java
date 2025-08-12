package com.charserzh.lmt.core.config.job;

import cn.hutool.core.lang.Pair;
import lombok.Data;

/**
 * 定时任务参数
 */
@Data
public class LmtTaskParam {

    private String bizSceneCode;

    private String bizId;

    private Long pageSize;

    private Long current;

    private String tailNums;

    private Pair<Long, Long> range;

    private boolean onlyFailed = true;
}
