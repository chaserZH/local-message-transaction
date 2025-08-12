package com.charserzh.lmt.core.eums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TransactionExecStatusEnum {

    WAIT(Integer.valueOf(0), "待执行"),
    SUCCESS(Integer.valueOf(1), "执行成功"),
    FAILED(Integer.valueOf(2), "执行失败");

    private final Integer code;

    private final String description;

    @JsonValue
    public Integer getCode() {
        return this.code;
    }

    public String getDescription() {
        return this.description;
    }

    TransactionExecStatusEnum(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    @JsonCreator
    public static TransactionExecStatusEnum codeOf(Integer code) {
        for (TransactionExecStatusEnum v : values()) {
            if (v.getCode().equals(code)) {
                return v;
            }
        }
        return null;
    }
}
