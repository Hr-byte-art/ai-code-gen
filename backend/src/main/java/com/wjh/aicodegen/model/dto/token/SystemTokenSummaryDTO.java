package com.wjh.aicodegen.model.dto.token;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 系统Token消耗汇总DTO
 *
 * @author AI Assistant
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemTokenSummaryDTO {

    /**
     * 系统总输入Token数量
     */
    private Long totalInputTokens;

    /**
     * 系统总输出Token数量
     */
    private Long totalOutputTokens;

    /**
     * 系统总Token数量
     */
    private Long totalTokens;

    /**
     * 总用户数
     */
    private Long totalUsers;

    /**
     * 总应用数
     */
    private Long totalApps;

    /**
     * 总调用次数
     */
    private Long totalCalls;

    /**
     * 按用途分组的Token统计
     * key: 用途(ROUTING/CODE_GENERATION/INPUT_SAFETY_CHECK)
     * value: Token数量
     */
    private Map<String, Long> tokenByPurpose;

    /**
     * 按模型分组的Token统计
     * key: 模型名称
     * value: Token数量
     */
    private Map<String, Long> tokenByModel;

    /**
     * 统计开始时间
     */
    private LocalDateTime statisticsStartTime;

    /**
     * 统计结束时间
     */
    private LocalDateTime statisticsEndTime;

    /**
     * 平均每用户Token消耗
     */
    private Double avgTokenPerUser;

    /**
     * 平均每应用Token消耗
     */
    private Double avgTokenPerApp;
}
