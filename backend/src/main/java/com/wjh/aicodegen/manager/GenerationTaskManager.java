package com.wjh.aicodegen.manager;

import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 生成任务状态机。
 *
 * 这里先使用内存状态，避免引入数据库迁移；状态事件通过现有 SSE 流广播给前端。
 */
@Slf4j
@Component
public class GenerationTaskManager {

    private final ConcurrentMap<Long, GenerationTaskState> states = new ConcurrentHashMap<>();

    public GenerationTaskState start(Long appId) {
        LocalDateTime now = LocalDateTime.now();
        GenerationTaskState previous = states.get(appId);
        int attempt = previous == null || previous.getAttempt() == null ? 1 : previous.getAttempt() + 1;
        GenerationTaskState state = GenerationTaskState.builder()
                .appId(appId)
                .status("generating")
                .stage("generating")
                .message("正在生成代码")
                .attempt(attempt)
                .startedAt(now)
                .updatedAt(now)
                .buildStatus("pending")
                .build();
        states.put(appId, state);
        log.info("生成任务开始: appId={}, attempt={}", appId, attempt);
        return state;
    }

    public GenerationTaskState markGenerating(Long appId, String message) {
        return update(appId, "generating", "generating", message, null, null, null);
    }

    public GenerationTaskState markValidating(Long appId, String message) {
        return update(appId, "validating", "validating", message, null, null, null);
    }

    public GenerationTaskState markBuilding(Long appId, String message) {
        return update(appId, "building", "building", message, null, null, "building");
    }

    public GenerationTaskState succeed(Long appId, String message, String outputPath) {
        return finish(appId, "succeeded", "succeeded", message, null, outputPath, "success");
    }

    public GenerationTaskState fail(Long appId, String message, String error) {
        return finish(appId, "failed", "failed", message, error, null, "failed");
    }

    public GenerationTaskState cancel(Long appId, String message) {
        return finish(appId, "cancelled", "cancelled", message, null, null, "cancelled");
    }

    public GenerationTaskState snapshot(Long appId) {
        GenerationTaskState state = states.get(appId);
        if (state != null) {
            return state;
        }
        LocalDateTime now = LocalDateTime.now();
        return GenerationTaskState.builder()
                .appId(appId)
                .status("idle")
                .stage("idle")
                .message("暂无运行中的生成任务")
                .attempt(0)
                .updatedAt(now)
                .build();
    }

    public String toStateEvent(GenerationTaskState state) {
        return JSONUtil.toJsonStr(Map.of(
                "type", "state",
                "data", state
        ));
    }

    private GenerationTaskState update(Long appId, String status, String stage, String message,
                                       String error, String outputPath, String buildStatus) {
        LocalDateTime now = LocalDateTime.now();
        GenerationTaskState state = states.compute(appId, (key, existing) -> {
            GenerationTaskState current = existing;
            if (current == null) {
                current = GenerationTaskState.builder()
                        .appId(appId)
                        .attempt(1)
                        .startedAt(now)
                        .buildStatus("pending")
                        .build();
            }
            current.setStatus(status);
            current.setStage(stage);
            current.setMessage(message);
            current.setError(error);
            if (outputPath != null) {
                current.setOutputPath(outputPath);
            }
            if (buildStatus != null) {
                current.setBuildStatus(buildStatus);
            }
            current.setUpdatedAt(now);
            return current;
        });
        log.debug("生成任务状态更新: appId={}, status={}, stage={}, message={}", appId, status, stage, message);
        return state;
    }

    private GenerationTaskState finish(Long appId, String status, String stage, String message,
                                       String error, String outputPath, String buildStatus) {
        GenerationTaskState state = update(appId, status, stage, message, error, outputPath, buildStatus);
        state.setFinishedAt(LocalDateTime.now());
        state.setUpdatedAt(state.getFinishedAt());
        log.info("生成任务结束: appId={}, status={}, message={}", appId, status, message);
        return state;
    }
}