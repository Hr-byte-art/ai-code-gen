package com.wjh.aicodegen.exception;

import lombok.Getter;

/**
 * @Author 王哈哈
 * @Date 2025/8/5 15:43:36
 * @Description 错误码枚举
 */
@Getter
public enum ErrorCode {

    SUCCESS(0, "请求成功"),
    PARAMS_ERROR(40000, "请求参数错误"),
    NOT_LOGIN_ERROR(40100, "未登录"),
    NO_AUTH_ERROR(40101, "无权限"),
    NOT_FOUND_ERROR(40400, "请求数据不存在"),
    FORBIDDEN_ERROR(40300, "禁止访问"),
    SYSTEM_ERROR(50000, "内部系统异常"),
    OPERATION_ERROR(50001, "操作失败");

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

