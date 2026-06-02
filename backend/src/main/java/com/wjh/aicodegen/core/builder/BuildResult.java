package com.wjh.aicodegen.core.builder;

import lombok.Builder;
import lombok.Data;

/**
 * 项目构建结果
 */
@Data
@Builder
public class BuildResult {

    private boolean success;

    private String command;

    private Integer exitCode;

    private String output;

    private long durationMs;

    private String errorMessage;

    public static BuildResult success(String command, Integer exitCode, String output, long durationMs) {
        return BuildResult.builder()
                .success(true)
                .command(command)
                .exitCode(exitCode)
                .output(output)
                .durationMs(durationMs)
                .build();
    }

    public static BuildResult failed(String command, Integer exitCode, String output, long durationMs, String errorMessage) {
        return BuildResult.builder()
                .success(false)
                .command(command)
                .exitCode(exitCode)
                .output(output)
                .durationMs(durationMs)
                .errorMessage(errorMessage)
                .build();
    }

    public String toErrorSummary() {
        StringBuilder summary = new StringBuilder();
        if (command != null) {
            summary.append("命令: ").append(command).append('\n');
        }
        if (exitCode != null) {
            summary.append("退出码: ").append(exitCode).append('\n');
        }
        if (durationMs > 0) {
            summary.append("耗时: ").append(durationMs).append("ms\n");
        }
        if (errorMessage != null && !errorMessage.isBlank()) {
            summary.append("错误: ").append(errorMessage).append('\n');
        }
        if (output != null && !output.isBlank()) {
            summary.append("输出:\n").append(output);
        }
        return summary.toString();
    }
}