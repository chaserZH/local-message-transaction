package com.charserzh.lmt.starter.test;

import com.charserzh.lmt.core.annotation.LMT;
import com.charserzh.lmt.core.callback.LTCallback;
import com.charserzh.lmt.core.model.CallbackResultValue;
import com.charserzh.lmt.core.model.StatusTransactionRecordEntity;

@LMT(bizSceneCode = "test_lmt")
public class TestCallback implements LTCallback {

    @Override
    public CallbackResultValue callback(StatusTransactionRecordEntity paramStatusTransactionRecordEntity) {
        // mock biz logic
        System.out.println("-------111111------");
        return CallbackResultValue.builder().result(true).build();
    }
}
