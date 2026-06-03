package com.wjh.aicodegen.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Agent 追踪链路摘要
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentTraceSummaryVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 追踪 ID */
    private String traceId;

    /** 关联应用 ID */
    private Long appId;

    /** 关联用户 ID */
    private Long userId;

    /** 链路整体状态 */
    private String status;

    /** 最终审查结果 */
    private String finalReviewResult;

    /** 最终审查评分 */
    private Integer finalReviewScore;

    /** 执行步骤数 */
    private Integer stepCount;

    /** 总耗时 */
    private Long totalDurationMs;

    /** 开始时间 */
    private LocalDateTime startTime;

    /** 结束时间 */
    private LocalDateTime endTime;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 是否存在异常 */
    private Boolean hasError;
}