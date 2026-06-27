package com.wjh.aicodegen.core.builder;

import lombok.Builder;
import lombok.Data;

/**
 * 带重试的构建结果
 */
@Data
@Builder
public class BuildRetryResult {

    private boolean success;

    private int retryCount;

    private String errorSummary;

    private String output;

    public static BuildRetryResult success(int retryCount, BuildResult buildResult) {
        return BuildRetryResult.builder()
                .success(true)
                .retryCount(retryCount)
                .output(buildResult != null ? buildResult.getOutput() : null)
                .build();
    }

    public static BuildRetryResult failed(int retryCount, String errorSummary) {
        return BuildRetryResult.builder()
                .success(false)
                .retryCount(retryCount)
                .errorSummary(errorSummary)
                .build();
    }
}