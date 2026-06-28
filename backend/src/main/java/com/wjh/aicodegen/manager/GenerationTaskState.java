package com.wjh.aicodegen.manager;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 代码生成任务状态快照。
 *
 * 状态只描述“当前任务是否完整可用”，不要和聊天文本流的网络生命周期混用。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerationTaskState {

    private Long appId;

    /** queued / generating / validating / building / succeeded / failed / cancelled / idle */
    private String status;

    /** 当前阶段，用于前端展示更细的进度 */
    private String stage;

    private String message;

    private String error;

    private Integer attempt;

    private String outputPath;

    private String buildStatus;

    private LocalDateTime startedAt;

    private LocalDateTime updatedAt;

    private LocalDateTime finishedAt;
}