package com.wjh.aicodegen.model.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.time.LocalDateTime;

import java.io.Serial;

import com.mybatisflex.core.keygen.KeyGenerators;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *  实体类。
 *
 * @author 王哈哈
 * @since 2025-08-06 10:50:59
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("vip_code")
public class VipCode implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @Id(keyType = KeyType.Generator , value = KeyGenerators.snowFlakeId)
    private Long id;

    /**
     * vipCode
     */
    @Column("vipCode")
    private String vipCode;

    /**
     * vipCode的兑换的VIP的有效时间（天）
     */
    @Column("effectiveDay")
    private Integer effectiveDay;

    /**
     * 该code的过期时间
     */
    @Column("expDate")
    private LocalDateTime expDate;

    /**
     * 是否删除
     */
    @Column(value = "idDelete" , isLogicDelete = true)
    private Integer idDelete;

    /**
     * 创建时间
     */
    @Column("createTime")
    private LocalDateTime createTime;

    /**
     * 修改时间
     */
    @Column("updateTime")
    private LocalDateTime updateTime;

    /**
     * 创建人
     */
    @Column("createUser")
    private Long createUser;

    /**
     * 使用人数
     */
    @Column("useNum")
    private Integer useNum;

    /**
     * 最大使用人数
     */
    @Column("maxUseNum")
    private Integer maxUseNum;
}
