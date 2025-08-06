package com.wjh.aicodegen.model.vo.user;

import com.mybatisflex.annotation.Column;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class UserVO implements Serializable {

    /**
     * id
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
     * 会员过期时间
     */
    @Column("vipExpireTime")
    private LocalDateTime vipExpireTime;

    /**
     * 会员兑换码
     */
    @Column("vipCode")
    private String vipCode;

    /**
     * 会员编号
     */
    @Column("vipNumber")
    private Long vipNumber;

    /**
     * 分享码
     */
    @Column("shareCode")
    private String shareCode;

    /**
     * 邀请用户 id
     */
    @Column("inviteUser")
    private Long inviteUser;

    private static final long serialVersionUID = 1L;
}
