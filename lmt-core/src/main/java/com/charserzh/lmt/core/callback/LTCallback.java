package com.charserzh.lmt.core.callback;

import com.charserzh.lmt.core.model.CallbackResultValue;
import com.charserzh.lmt.core.model.StatusTransactionRecordEntity;

public interface LTCallback {
    /**
     * 回调方法
     *
     * @param paramStatusTransactionRecordEntity 本地消息事务记录实体
     * @return 回调结果
     */
    CallbackResultValue callback(StatusTransactionRecordEntity paramStatusTransactionRecordEntity);
}