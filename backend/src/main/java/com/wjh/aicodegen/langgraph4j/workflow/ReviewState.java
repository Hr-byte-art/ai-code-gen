package com.wjh.aicodegen.langgraph4j.workflow;

import com.wjh.aicodegen.agent.model.ReviewResult;
import lombok.Data;

/**
 * 审查工作流状态
 */
@Data
public class ReviewState {

    /** 原始代码 */
    private String code;

    /** 当前代码（可能经过优化） */
    private String currentCode;

    /** 应用 ID */
    private Long appId;

    /** 审查结果 */
    private ReviewResult reviewResult;

    /** 重试次数 */
    private int retryCount = 0;

    /** 最大重试次数 */
    private static final int MAX_RETRIES = 3;

    /** 工作流是否完成 */
    private boolean completed = false;

    /** 最终代码 */
    private String finalCode;

    public ReviewState(String code, Long appId) {
        this.code = code;
        this.currentCode = code;
        this.appId = appId;
    }

    public boolean shouldRetry() {
        return reviewResult != null
                && reviewResult.needsRetry()
                && retryCount < MAX_RETRIES;
    }

    public void incrementRetry() {
        retryCount++;
    }

    public void markCompleted(String finalCode) {
        this.completed = true;
        this.finalCode = finalCode;
    }
}
