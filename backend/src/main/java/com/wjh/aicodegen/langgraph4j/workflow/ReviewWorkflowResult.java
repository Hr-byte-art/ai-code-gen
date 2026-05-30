package com.wjh.aicodegen.langgraph4j.workflow;

import com.wjh.aicodegen.agent.model.ReviewResult;
import lombok.Data;

/**
 * 审查工作流结果
 */
@Data
public class ReviewWorkflowResult {

    /** 最终代码 */
    private String finalCode;

    /** 审查结果 */
    private ReviewResult reviewResult;

    /** 重试次数 */
    private int retryCount;

    /** 结果状态 */
    private Status status;

    public enum Status {
        PASSED,             // 审查通过
        PASSED_WITH_WARNINGS, // 审查通过但有警告
        FAILED              // 审查失败（超过重试次数）
    }

    public static ReviewWorkflowResult passed(String code, ReviewResult result, int retryCount) {
        ReviewWorkflowResult r = new ReviewWorkflowResult();
        r.setFinalCode(code);
        r.setReviewResult(result);
        r.setRetryCount(retryCount);
        r.setStatus(Status.PASSED);
        return r;
    }

    public static ReviewWorkflowResult passedWithWarnings(String code, ReviewResult result, int retryCount) {
        ReviewWorkflowResult r = new ReviewWorkflowResult();
        r.setFinalCode(code);
        r.setReviewResult(result);
        r.setRetryCount(retryCount);
        r.setStatus(Status.PASSED_WITH_WARNINGS);
        return r;
    }

    public static ReviewWorkflowResult failed(String code, ReviewResult result, int retryCount) {
        ReviewWorkflowResult r = new ReviewWorkflowResult();
        r.setFinalCode(code);
        r.setReviewResult(result);
        r.setRetryCount(retryCount);
        r.setStatus(Status.FAILED);
        return r;
    }

    public boolean isPassed() {
        return status == Status.PASSED || status == Status.PASSED_WITH_WARNINGS;
    }
}
