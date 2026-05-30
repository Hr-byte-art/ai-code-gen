package com.wjh.aicodegen.ai.model.message;

import com.wjh.aicodegen.agent.model.ReviewResult;
import com.wjh.aicodegen.model.enums.StreamMessageTypeEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 代码审查结果消息（通过 SSE 推送给前端）
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ReviewResultMessage extends StreamMessage {

    private boolean passed;
    private int score;
    private String severity;
    private String summary;

    public ReviewResultMessage(ReviewResult result) {
        super(StreamMessageTypeEnum.REVIEW_RESULT.getValue());
        this.passed = Boolean.TRUE.equals(result.getPassed());
        this.score = result.getScore() != null ? result.getScore() : 0;
        this.severity = result.getSeverity() != null ? result.getSeverity() : "info";
        this.summary = result.getSummary() != null ? result.getSummary() : "";
    }
}
