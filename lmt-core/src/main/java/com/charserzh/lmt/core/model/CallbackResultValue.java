package com.charserzh.lmt.core.model;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * 回调结果值
 *
 * @author charserzh
 * @since 2023/11/15
 */
@Builder
@Data
public class CallbackResultValue implements Serializable {

    private boolean result;

    private String message;

    private String data;
}
