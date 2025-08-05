package com.wjh.aicodegen.exception;

import lombok.Getter;

/**
 * @Author 王哈哈
 * @Date 2025/8/5 15:43:36
 * @Description 错误码枚举
 */
@Getter
public enum ErrorCode {

    SUCCESS(0, "REQUEST SUCCESSFUL"),
    PARAMS_ERROR(40000, "REQUEST PARAMETER ERROR"),
    NOT_LOGIN_ERROR(40100, "NOT LOGGED IN"),
    NO_AUTH_ERROR(40101, "NO PERMISSION"),
    NOT_FOUND_ERROR(40400, "REQUEST DATA DOES NOT EXIST"),
    FORBIDDEN_ERROR(40300, "PROHIBIT ACCESS"),
    SYSTEM_ERROR(50000, "INTERNAL SYSTEM ABNORMALITY"),
    OPERATION_ERROR(50001, "OPERATION FAILED");

    /**
     * 状态码
     */
    private final int code;

    /**
     * 信息
     */
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}

