package com.wjh.aicodegen.model.enums;

import lombok.Getter;

/**
 * @Author 王哈哈
 * @Date 2025/8/5 17:03:17
 * @Description 角色枚举
 */
@Getter
public enum UserRoleEnum {
    USER("用户","user"),
    ADMIN("管理员","admin"),
    VIP("会员","vip");
    private String role;
    private String value;

    UserRoleEnum(String role, String value) {
        this.role = role;
        this.value = value;
    }

    /**
     * 根据value获取枚举
     * @param value
     * @return
     */
    public static UserRoleEnum getEnumByValue(String value) {
        //判空
        if (value == null) {
            return null;
        }
        for (UserRoleEnum enumValue : UserRoleEnum.values()){
            if (enumValue.value.equals(value)) {
                return enumValue;
            }
        }
        return null;
    }
}
