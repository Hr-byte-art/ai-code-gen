package com.wjh.aicodegen.model.vo.app;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 应用代码审查报告
 */
@Data
@Builder
public class AppReviewReportVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long appId;

    private String status;

    private Integer score;

    private String result;

    private List<String> issues;

    private String errorMessage;

    private Integer retryCount;

    private String traceId;

    private LocalDateTime updatedTime;
}