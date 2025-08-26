package com.wjh.aicodegen.model.dto.user;

import lombok.Data;

/**
 * @Author 王哈哈
 * @Date 2025/8/27 01:01:12
 * @Description 修改密码
 */
@Data
public class ChangePasswordRequest {

    /**
     * 旧密码
     */
    private String oldPassword;

    /**
     * 新密码
     */
    private String newPassword;

    /**
     * 确认密码
     */
    private String confirmPassword;

}
