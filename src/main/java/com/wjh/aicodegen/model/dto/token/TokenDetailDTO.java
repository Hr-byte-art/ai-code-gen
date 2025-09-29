package com.wjh.aicodegen.model.dto.token;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Token使用详情DTO
 *
 * @author AI Assistant
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenDetailDTO {

    /**
     * 记录ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String userName;

    /**
     * 应用ID
     */
    private Long appId;

    /**
     * 应用名称
     */
    private String appName;

    /**
     * 模型名称
     */
    private String modelName;

    /**
     * AI调用用途
     */
    private String aiCallPurpose;

    /**
     * 输入Token数量
     */
    private Integer inputTokens;

    /**
     * 输出Token数量
     */
    private Integer outputTokens;

    /**
     * 总Token数量
     */
    private Integer totalTokens;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 用途描述
     */
    private String purposeDescription;
}
