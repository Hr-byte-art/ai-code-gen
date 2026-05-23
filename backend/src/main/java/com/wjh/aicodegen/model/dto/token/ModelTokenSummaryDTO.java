package com.wjh.aicodegen.model.dto.token;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 模型Token消耗汇总DTO
 *
 * @author AI Assistant
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelTokenSummaryDTO {

    /**
     * 模型名称
     */
    private String modelName;

    /**
     * 模型显示名称
     */
    private String modelDisplayName;

    /**
     * 总输入Token数量
     */
    private Long totalInputTokens;

    /**
     * 总输出Token数量
     */
    private Long totalOutputTokens;

    /**
     * 总Token数量
     */
    private Long totalTokens;

    /**
     * 调用次数
     */
    private Long callCount;

    /**
     * 使用用户数
     */
    private Long userCount;

    /**
     * 使用应用数
     */
    private Long appCount;

    /**
     * 平均每次调用Token数
     */
    private Double avgTokensPerCall;

    /**
     * 平均每用户Token数
     */
    private Double avgTokensPerUser;

    /**
     * 最早使用时间
     */
    private LocalDateTime firstUsedTime;

    /**
     * 最近使用时间
     */
    private LocalDateTime lastUsedTime;

    /**
     * 在模型排行中的排名
     */
    private Integer ranking;

    /**
     * 占总Token的百分比
     */
    private Double percentage;

    /**
     * 主要用途分布
     * 格式：用途1:百分比,用途2:百分比
     */
    private String purposeDistribution;

    /**
     * 模型类型/厂商
     * 例如：OpenAI, Claude等
     */
    private String modelProvider;

    /**
     * 估算成本（USD）
     */
    private Double estimatedCost;
}
