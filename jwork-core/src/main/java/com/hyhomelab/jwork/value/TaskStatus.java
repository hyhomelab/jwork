package com.hyhomelab.jwork.value;

import lombok.Getter;

/**
 * @author hyhomelab
 * @email hyhomelab@hotmail.com
 * @date 2025/4/29 13:57
 */
@Getter
public enum TaskStatus {
    NOT_TRIGGERED("not_triggered"), // 未被触发，不会加入队列
    PENDING("pending"), // 等待被执行
    RUNNING("running"),
    SUCCESS("success"),
    FAILED("failed");


    private final String value;

    TaskStatus(String value) {
        this.value = value;
    }


    public static TaskStatus fromValue(String status) {
        for(var v: values()){
            if(v.value.equals(status)){
                return v;
            }
        }
        throw new IllegalArgumentException("unsupported status string: %s".formatted(status));
    }
}
