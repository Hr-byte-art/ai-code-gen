package com.wjh.aicodegen.model.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.keygen.KeyGenerators;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户配额配置 实体类。
 *
 * @author 王哈哈
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("user_quota")
public class UserQuota implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;

    /**
     * 角色: user/vip/admin
     */
    @Column("user_role")
    private String userRole;

    /**
     * 每日生成次数上限
     */
    @Column("daily_gen_limit")
    private Integer dailyGenLimit;

    /**
     * 每月生成次数上限
     */
    @Column("monthly_gen_limit")
    private Integer monthlyGenLimit;

    /**
     * 每日 Token 消耗上限
     */
    @Column("daily_token_limit")
    private Integer dailyTokenLimit;

    /**
     * 每月 Token 消耗上限
     */
    @Column("monthly_token_limit")
    private Integer monthlyTokenLimit;

    @Column("create_time")
    private LocalDateTime createTime;

    @Column("update_time")
    private LocalDateTime updateTime;
}
