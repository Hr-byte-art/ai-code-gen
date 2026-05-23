package com.wjh.aicodegen.model.vo.user;

import com.mybatisflex.annotation.Column;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @author 王哈哈
 */
@Data
public class LoginUserVO implements Serializable {

    /**
     * 用户 id
     */
    private Long id;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 用户简介
     */
    private String userProfile;

    /**
     * 用户角色：user/admin
     */
    private String userRole;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 会员过期时间
     */
    private LocalDateTime vipExpireTime;

    /**
     * 会员编号
     */
    private Long vipNumber;

    /**
     * 分享码
     */
    private String shareCode;

    /**
     * 用户积分
     */
    private Integer integral;

    /**
     * 最近签到时间
     */
    private LocalDateTime recentlySignedIn;

    @Serial
    private static final long serialVersionUID = 1L;

}
