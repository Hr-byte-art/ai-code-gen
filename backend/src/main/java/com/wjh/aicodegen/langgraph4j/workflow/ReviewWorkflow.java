package com.wjh.aicodegen.langgraph4j.workflow;

import cn.hutool.json.JSONUtil;
import com.wjh.aicodegen.agent.CodeOptimizerAgent;
import com.wjh.aicodegen.agent.ReviewAgent;
import com.wjh.aicodegen.agent.model.ReviewResult;
import com.wjh.aicodegen.model.entity.AgentTrace;
import com.wjh.aicodegen.monitor.MonitorContextHolder;
import com.wjh.aicodegen.monitor.MonitorContext;
import com.wjh.aicodegen.service.AgentTraceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 代码审查工作流
 *
 * 流程：reviewer → (pass → END | fail → optimizer → reviewer)
 * 最多重试 3 次
 *
 * 使用 LangGraph4j 风格的节点编排，但不依赖 LangGraph4j 框架
 * （避免引入复杂的图状态管理，保持简单可控）
 */
@Slf4j
@Component
public class ReviewWorkflow {

    private static final Pattern CODE_FENCE_PATTERN = Pattern.compile("```(?:[a-zA-Z0-9_+-]+)?\\n([\\s\\S]*?)\\n```", Pattern.MULTILINE);
    private static final List<String> VALID_SEVERITIES = List.of("critical", "warning", "info");
    private static final int MAX_RETRIES = 3;

    private final ReviewAgent reviewAgent;
    private final CodeOptimizerAgent codeOptimizerAgent;

    @Resource
    private AgentTraceService agentTraceService;

    public ReviewWorkflow(@Lazy ReviewAgent reviewAgent, @Lazy CodeOptimizerAgent codeOptimizerAgent) {
        this.reviewAgent = reviewAgent;
        this.codeOptimizerAgent = codeOptimizerAgent;
    }

    /**
     * 执行审查工作流
     *
     * @param code   待审查的代码
     * @param appId  应用 ID
     * @return 审查结果（包含最终代码和审查信息）
     */
    public ReviewWorkflowResult execute(String code, Long appId) {
        String traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        String currentCode = code;
        int retryCount = 0;
        ReviewResult lastReviewResult = null;

        log.info("审查工作流开始: appId={}, traceId={}", appId, traceId);

        while (retryCount <= MAX_RETRIES) {
            // ┌─────────┐
            // │ Reviewer │
            // └─────────┘
            lastReviewResult = executeReviewer(currentCode, appId, traceId);

            // 审查通过 → END
            if (Boolean.TRUE.equals(lastReviewResult.getPassed())) {
                log.info("审查通过: appId={}, score={}, retryCount={}", appId, lastReviewResult.getScore(), retryCount);
                return ReviewWorkflowResult.passed(currentCode, lastReviewResult, retryCount);
            }

            // 非严重问题 → END（不重试）
            if (!lastReviewResult.needsRetry()) {
                log.info("审查发现问题（非严重），跳过优化: appId={}, severity={}", appId, lastReviewResult.getSeverity());
                return ReviewWorkflowResult.passedWithWarnings(currentCode, lastReviewResult, retryCount);
            }

            // 超过最大重试次数 → END
            if (retryCount >= MAX_RETRIES) {
                log.warn("审查重试次数已达上限: appId={}, retryCount={}", appId, retryCount);
                return ReviewWorkflowResult.failed(currentCode, lastReviewResult, retryCount);
            }

            // ┌───────────┐
            // │ Optimizer  │
            // └───────────┘
            currentCode = executeOptimizer(currentCode, lastReviewResult, appId, traceId);
            retryCount++;

            // 循环回到 Reviewer
            log.info("优化完成，重新审查: appId={}, retryCount={}", appId, retryCount);
        }

        // 不应该到达这里
        return ReviewWorkflowResult.failed(currentCode, lastReviewResult, retryCount);
    }

    /**
     * 执行审查节点
     */
    private ReviewResult executeReviewer(String code, Long appId, String traceId) {
        LocalDateTime startTime = LocalDateTime.now();
        long startMs = System.currentTimeMillis();
        String status = "success";
        String errorMsg = null;
        ReviewResult result = null;

        try {
            result = sanitizeReviewResult(reviewAgent.reviewCode(code));
            if (result == null) {
                log.warn("Reviewer 节点返回空或不完整结果，按严重问题处理: appId={}", appId);
                result = buildFallbackReviewResult("审查结果为空或字段缺失，按严重问题处理");
            }
            return result;
        } catch (Exception e) {
            status = "error";
            errorMsg = safeErrorMessage(e);
            log.error("Reviewer 节点异常: appId={}, error={}", appId, errorMsg);
            result = buildFallbackReviewResult("Reviewer 节点异常，按严重问题处理: " + errorMsg);
            return result;
        } finally {
            long duration = System.currentTimeMillis() - startMs;
            saveTrace(AgentTrace.builder()
                    .traceId(traceId)
                    .agentName("reviewer")
                    .appId(appId)
                    .userId(getCurrentUserId())
                    .modelName("deepseek-chat")
                    .status(status)
                    .reviewResult(result != null ? (Boolean.TRUE.equals(result.getPassed()) ? "passed" : "failed") : null)
                    .reviewScore(result != null ? result.getScore() : null)
                    .issues(result != null && result.getIssues() != null ? JSONUtil.toJsonStr(result.getIssues()) : null)
                    .startTime(startTime)
                    .endTime(LocalDateTime.now())
                    .durationMs(duration)
                    .errorMessage(errorMsg)
                    .build());
        }
    }

    /**
     * 执行优化节点
     */
    private String executeOptimizer(String code, ReviewResult reviewResult, Long appId, String traceId) {
        LocalDateTime startTime = LocalDateTime.now();
        long startMs = System.currentTimeMillis();
        String status = "success";
        String errorMsg = null;
        String result = null;

        try {
            String prompt = buildOptimizationPrompt(code, reviewResult);
            result = sanitizeOptimizedCode(codeOptimizerAgent.optimizeCode(prompt));
            if (result == null) {
                log.warn("Optimizer 节点返回空或不合法结果，保留原始代码: appId={}", appId);
                return code;
            }
            return result;
        } catch (Exception e) {
            status = "error";
            errorMsg = safeErrorMessage(e);
            log.error("Optimizer 节点异常: appId={}, error={}", appId, errorMsg);
            return code;
        } finally {
            long duration = System.currentTimeMillis() - startMs;
            saveTrace(AgentTrace.builder()
                    .traceId(traceId)
                    .agentName("optimizer")
                    .appId(appId)
                    .userId(getCurrentUserId())
                    .modelName("deepseek-chat")
                    .status(status)
                    .startTime(startTime)
                    .endTime(LocalDateTime.now())
                    .durationMs(duration)
                    .errorMessage(errorMsg)
                    .build());
        }
    }

    private ReviewResult sanitizeReviewResult(ReviewResult result) {
        if (result == null) {
            return null;
        }
        if (result.getPassed() == null || result.getSeverity() == null || result.getSeverity().isBlank()
                || result.getScore() == null || result.getSummary() == null || result.getSummary().isBlank()) {
            return null;
        }

        String severity = result.getSeverity().trim().toLowerCase();
        if (!VALID_SEVERITIES.contains(severity)) {
            return null;
        }

        result.setSeverity(severity);
        if (result.getIssues() == null) {
            result.setIssues(Collections.emptyList());
        }
        if (result.getSuggestions() == null) {
            result.setSuggestions(Collections.emptyList());
        }
        return result;
    }

    private ReviewResult buildFallbackReviewResult(String summary) {
        return ReviewResult.builder()
                .passed(false)
                .severity("critical")
                .score(0)
                .summary(summary)
                .issues(List.of(summary))
                .suggestions(List.of("请检查审查 Agent / 优化 Agent 的输出格式后重新生成"))
                .build();
    }

    private String sanitizeOptimizedCode(String rawOutput) {
        if (rawOutput == null || rawOutput.isBlank()) {
            return null;
        }

        String trimmed = rawOutput.trim();
        Matcher matcher = CODE_FENCE_PATTERN.matcher(trimmed);
        List<String> blocks = new ArrayList<>();
        while (matcher.find()) {
            String block = matcher.group(1);
            if (block != null && !block.isBlank()) {
                blocks.add(block.trim());
            }
        }

        if (!blocks.isEmpty()) {
            return String.join("\n\n", blocks).trim();
        }

        return trimmed;
    }

    private String buildOptimizationPrompt(String code, ReviewResult reviewResult) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("## 原始代码\n\n").append(code);
        prompt.append("\n\n## 审查结果\n\n");

        if (reviewResult.getIssues() != null && !reviewResult.getIssues().isEmpty()) {
            prompt.append("### 必须修复的问题\n");
            for (String issue : reviewResult.getIssues()) {
                prompt.append("- ").append(issue).append("\n");
            }
        }
        if (reviewResult.getSuggestions() != null && !reviewResult.getSuggestions().isEmpty()) {
            prompt.append("\n### 改进建议\n");
            for (String suggestion : reviewResult.getSuggestions()) {
                prompt.append("- ").append(suggestion).append("\n");
            }
        }
        prompt.append("\n请直接输出修复后的完整代码，只保留代码内容，不要输出解释、说明、Markdown 代码块或额外文本。");
        return prompt.toString();
    }

    private String safeErrorMessage(Exception e) {
        String message = e.getMessage();
        if (message == null || message.isBlank()) {
            return e.getClass().getSimpleName();
        }
        return message;
    }

    private void saveTrace(AgentTrace trace) {
        try {
            agentTraceService.save(trace);
        } catch (Exception e) {
            log.warn("保存追踪记录失败: {}", e.getMessage());
        }
    }

    private Long getCurrentUserId() {
        try {
            MonitorContext ctx = MonitorContextHolder.getContext();
            if (ctx != null && ctx.getUserId() != null) {
                return Long.parseLong(ctx.getUserId());
            }
        } catch (Exception ignored) {}
        return null;
    }
}
