package com.wjh.aicodegen.observability.model.entity;

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
 * AI Token 使用统计表 实体类。
 *
 * @author 王哈哈
 * @since 2025-09-23 22:04:54
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("token_usage_record")
public class TokenUsage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;

    /**
     * 用户ID
     */
    @Column("userId")
    private Long userId;

    /**
     * 应用ID
     */
    @Column("appId")
    private Long appId;

    /**
     * 模型名称
     */
    @Column("modelName")
    private String modelName;

    /**
     * AI调用用途（如ROUTING/CODE_GENERATION/IMAGE_SEARCH等）
     * 数据库字段映射到aiCallPurpose
     */
    @Column("aiCallPurpose")
    private String appType;

    /**
     * 请求 Token 数量
     */
    @Column("inputTokens")
    private Integer requestTokenCount;

    /**
     * 创建时间
     */
    @Column("createTime")
    private LocalDateTime createTime;

    /**
     * 响应 Token 数量
     */
    @Column("outputTokens")
    private Integer responseTokenCount;

    /**
     * 总消耗 Token 数量
     */
    @Column("totalTokens")
    private Integer totalTokenCount;

}
