package com.community.waste.common;

/** 业务异常，返回 400 */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
