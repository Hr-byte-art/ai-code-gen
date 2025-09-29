package com.wjh.aicodegen.observability.model.enums;

import com.wjh.aicodegen.model.enums.UserRoleEnum;
import lombok.Getter;
import lombok.Setter;

/**
 * @Author 王哈哈
 * @Date 2025/9/23 21:38:59
 * @Description
 */
@Getter
public enum TokenType {
    Request("请求" , "request"),
    Response("响应" , "response");
    private String value;
    private String text;

    TokenType(String text, String value) {
        this.text = text;
        this.value = value;
    }
    /**
     * 根据value获取枚举
     * @param value
     * @return
     */
    public static TokenType getEnumByValue(String value) {
        if (value == null){
            return null;
        }
        for (TokenType item : TokenType.values()){
            if (item.value.equals(value)){
                return item;
            }
        }
        return null;
    }
}
