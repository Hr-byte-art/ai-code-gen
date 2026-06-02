package com.wjh.aicodegen.agent.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 代码审查结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResult {

    /** 是否通过审查 */
    private Boolean passed;

    /** 严重程度: critical / warning / info */
    private String severity;

    /** 发现的问题列表 */
    private List<String> issues;

    /** 改进建议 */
    private List<String> suggestions;

    /** 整体评分 0-100 */
    private Integer score;

    /** 审查摘要 */
    private String summary;

    /**
     * 是否需要重试（严重问题）
     */
    public boolean needsRetry() {
        return !Boolean.TRUE.equals(passed)
                && (severity == null || severity.isBlank() || "critical".equalsIgnoreCase(severity));
    }
}
