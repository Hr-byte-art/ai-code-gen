package com.wjh.aicodegen.agent;

import cn.hutool.json.JSONUtil;
import com.wjh.aicodegen.agent.model.ReviewResult;
import com.wjh.aicodegen.core.parser.CodeParserExecutor;
import com.wjh.aicodegen.core.saver.CodeFileSaverExecutor;
import com.wjh.aicodegen.model.entity.AgentTrace;
import com.wjh.aicodegen.model.enums.CodeGenTypeEnum;
import com.wjh.aicodegen.service.AgentTraceService;
import com.wjh.aicodegen.monitor.MonitorContextHolder;
import com.wjh.aicodegen.monitor.MonitorContext;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 多 Agent 编排器
 * 协调代码生成、审查、优化的完整流程
 * 记录每次执行的追踪信息
 */
@Slf4j
@Component
public class AgentOrchestrator {

    private final ReviewAgent reviewAgent;
    private final CodeOptimizerAgent codeOptimizerAgent;

    @Resource
    private AgentTraceService agentTraceService;

    @Resource
    private com.wjh.aicodegen.observability.MetricsService metricsService;

    /** 最大审查重试次数 */
    private static final int MAX_REVIEW_RETRIES = 3;

    public AgentOrchestrator(@Lazy ReviewAgent reviewAgent, @Lazy CodeOptimizerAgent codeOptimizerAgent) {
        this.reviewAgent = reviewAgent;
        this.codeOptimizerAgent = codeOptimizerAgent;
    }

    /**
     * 执行完整的代码审查和优化流程
     *
     * @param codeContent 生成的代码内容
     * @param codeGenType 代码生成类型
     * @param appId       应用ID
     * @return 最终的代码内容（可能经过优化）
     */
    public String reviewAndOptimize(String codeContent, CodeGenTypeEnum codeGenType, Long appId) {
        String traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        String currentCode = codeContent;
        int retryCount = 0;

        while (retryCount < MAX_REVIEW_RETRIES) {
            log.info("代码审查 Agent 开始审查，应用ID: {}, 第 {} 次审查", appId, retryCount + 1);

            // 1. 审查代码（带追踪）
            ReviewResult reviewResult = reviewWithTrace(currentCode, appId, traceId);
            log.info("审查结果 - 通过: {}, 评分: {}, 严重程度: {}",
                    reviewResult.getPassed(), reviewResult.getScore(), reviewResult.getSeverity());

            // 2. 如果审查通过，直接返回
            if (Boolean.TRUE.equals(reviewResult.getPassed())) {
                log.info("代码审查通过，应用ID: {}, 评分: {}", appId, reviewResult.getScore());
                return currentCode;
            }

            // 3. 如果是严重问题，需要优化
            if (reviewResult.needsRetry()) {
                log.warn("发现严重问题，启动代码优化 Agent，应用ID: {}", appId);
                log.warn("问题列表: {}", reviewResult.getIssues());

                // 4. 调用优化 Agent（带追踪）
                String optimizationPrompt = buildOptimizationPrompt(currentCode, reviewResult);
                currentCode = optimizeWithTrace(optimizationPrompt, appId, traceId);
                log.info("代码优化完成，应用ID: {}", appId);

                retryCount++;
            } else {
                // 非严重问题，记录警告但不重试
                log.warn("代码审查发现问题（非严重），应用ID: {}, 问题: {}",
                        appId, reviewResult.getIssues());
                return currentCode;
            }
        }

        log.warn("代码审查重试次数已达上限，应用ID: {}, 使用最后一次优化的版本", appId);
        return currentCode;
    }

    /**
     * 带追踪的审查调用
     */
    private ReviewResult reviewWithTrace(String code, Long appId, String traceId) {
        LocalDateTime startTime = LocalDateTime.now();
        long startMs = System.currentTimeMillis();
        ReviewResult result = null;
        String status = "success";
        String errorMsg = null;

        try {
            result = reviewAgent.reviewCode(code);
            return result;
        } catch (Exception e) {
            status = "error";
            errorMsg = e.getMessage();
            log.error("ReviewAgent 调用异常: {}", e.getMessage());
            throw e;
        } finally {
            long duration = System.currentTimeMillis() - startMs;
            // 记录 Prometheus 指标
            metricsService.recordAgentExecution("reviewer", status, duration);
            saveTrace(AgentTrace.builder()
                    .traceId(traceId)
                    .agentName("review")
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
     * 带追踪的优化调用
     */
    private String optimizeWithTrace(String prompt, Long appId, String traceId) {
        LocalDateTime startTime = LocalDateTime.now();
        long startMs = System.currentTimeMillis();
        String status = "success";
        String errorMsg = null;
        String result = null;

        try {
            result = codeOptimizerAgent.optimizeCode(prompt);
            return result;
        } catch (Exception e) {
            status = "error";
            errorMsg = e.getMessage();
            log.error("CodeOptimizerAgent 调用异常: {}", e.getMessage());
            throw e;
        } finally {
            long duration = System.currentTimeMillis() - startMs;
            // 记录 Prometheus 指标
            metricsService.recordAgentExecution("optimizer", status, duration);
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

    /**
     * 保存追踪记录（异步，不阻塞主流程）
     */
    private void saveTrace(AgentTrace trace) {
        try {
            agentTraceService.save(trace);
        } catch (Exception e) {
            log.warn("保存 Agent 追踪记录失败: {}", e.getMessage());
        }
    }

    /**
     * 获取当前用户 ID
     */
    private Long getCurrentUserId() {
        try {
            MonitorContext ctx = MonitorContextHolder.getContext();
            if (ctx != null && ctx.getUserId() != null) {
                return Long.parseLong(ctx.getUserId());
            }
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * 构建优化提示词
     */
    private String buildOptimizationPrompt(String codeContent, ReviewResult reviewResult) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("## 原始代码\n\n");
        prompt.append(codeContent);
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

        prompt.append("\n请根据以上审查结果，修复代码中的问题。");

        return prompt.toString();
    }

    /**
     * 保存优化后的代码
     */
    public File saveOptimizedCode(String codeContent, CodeGenTypeEnum codeGenType, Long appId) {
        try {
            Object parsedResult = CodeParserExecutor.executeParser(codeContent, codeGenType);
            File savedDir = CodeFileSaverExecutor.executeSaver(parsedResult, codeGenType, appId);
            log.info("优化后的代码已保存，路径: {}", savedDir.getAbsolutePath());
            return savedDir;
        } catch (Exception e) {
            log.error("保存优化后的代码失败，应用ID: {}", appId, e);
            throw e;
        }
    }
}
