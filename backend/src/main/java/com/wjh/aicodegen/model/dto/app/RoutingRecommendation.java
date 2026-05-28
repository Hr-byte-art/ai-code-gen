package com.wjh.aicodegen.model.dto.app;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 路由推荐结果
 * AI 预判用户需求应该使用哪种生成模式，并给出理由
 *
 * @author 王哈哈
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoutingRecommendation {

    /**
     * 推荐的代码生成类型
     */
    private String recommendedType;

    /**
     * 推荐类型的名称（用于展示）
     */
    private String recommendedName;

    /**
     * 推荐理由
     */
    private String reason;

    /**
     * 是否推荐全栈模式
     */
    private boolean fullstack;

    /**
     * 推荐类型的积分消耗
     */
    private Integer pointCost;

    /**
     * 备选方案（纯前端）
     */
    private String alternativeType;

    /**
     * 备选方案名称
     */
    private String alternativeName;

    /**
     * 备选方案积分消耗
     */
    private Integer alternativePointCost;
}
