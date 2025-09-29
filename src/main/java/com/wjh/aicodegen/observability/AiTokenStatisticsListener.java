package com.wjh.aicodegen.observability;

import com.wjh.aicodegen.model.enums.AiCallPurposeEnum;
import com.wjh.aicodegen.monitor.GlobalContextStorage;
import com.wjh.aicodegen.monitor.MonitorContext;
import com.wjh.aicodegen.monitor.MonitorContextHolder;
import com.wjh.aicodegen.monitor.RequestContextManager;
import com.wjh.aicodegen.observability.model.entity.TokenUsage;
import com.wjh.aicodegen.service.AppService;
import com.wjh.aicodegen.service.TokenUsageService;
import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import jakarta.annotation.Resource;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * @Author 王哈哈
 * @Date 2025/9/23 21:06:09
 * @Description 模型消耗量
 */
@Component
@Slf4j
public class AiTokenStatisticsListener implements ChatModelListener {
    @Resource
    private AppService appService;
    @Resource
    private TokenUsageService tokenUsageService;
    @Resource
    private RequestContextManager requestContextManager;

    // 用于存储请求开始时间的键
    private static final String REQUEST_START_TIME_KEY = "request_start_time";
    // 用于监控上下文传递（因为请求和响应事件的触发不是同一个线程）
    private static final String MONITOR_CONTEXT_KEY = "monitor_context";

    @Override
    public void onRequest(ChatModelRequestContext requestContext) {
        log.info("监控 onRequest: {}", requestContext);
        requestContext.attributes().put(REQUEST_START_TIME_KEY, Instant.now());

        // 生成唯一的请求ID
        String requestId = UUID.randomUUID().toString();
        requestContext.attributes().put("request_id", requestId);

        // 多种方式获取监控上下文
        MonitorContext context = getMonitorContext();
        if (context == null) {
            log.info("MonitorContext not available in onRequest, creating default context");
            // 如果无法获取上下文，创建一个默认的（用于统计但不包含用户信息）
            context = MonitorContext.builder()
                    .userId("unknown")
                    .appId("unknown")
                    .aiCallPurpose("UNKNOWN")
                    .build();
        } else {
            log.info("成功获取MonitorContext: userId={}, appId={}, aiCallPurpose={}",
                    context.getUserId(), context.getAppId(), context.getAiCallPurpose());

            // 检测是否是工具调用，并临时设置用途
            String originalPurpose = context.getAiCallPurpose();
            String detectedPurpose = detectAiCallPurpose(requestContext, context);
            if (!detectedPurpose.equals(originalPurpose)) {
                // 创建一个临时的上下文副本，不修改原始上下文
                context = MonitorContext.builder()
                        .userId(context.getUserId())
                        .appId(context.getAppId())
                        .aiCallPurpose(detectedPurpose)
                        .build();
            }
        }

        // 将上下文存储到请求ID映射中
        requestContextManager.storeContext(requestId, context);

        // 将上下文存储到请求属性中，确保跨线程传递
        requestContext.attributes().put(MONITOR_CONTEXT_KEY, context);

        // 重新设置到ThreadLocal，确保当前线程可以访问
        MonitorContextHolder.setContext(context);

    }

    /**
     * 检测AI调用的具体用途
     * 基于线程栈和调用上下文判断调用类型
     */
    private String detectAiCallPurpose(ChatModelRequestContext requestContext, MonitorContext currentContext) {
        try {
            // 通过调用栈分析检测具体用途
            String stackTrace = getStackTraceInfo();

            // 检测护轨机制调用（优先级最高）
            if (stackTrace.contains("Guardrail") || stackTrace.contains("SafetyCheck") ||
                    stackTrace.contains("InputGuardrail")) {
                return AiCallPurposeEnum.INPUT_SAFETY_CHECK.getCode();
            }

            // 检测路由服务调用
            if (stackTrace.contains("AiCodeGenTypeRoutingService") ||
                    stackTrace.contains("routeCodeGenType")) {
                return AiCallPurposeEnum.ROUTING.getCode();
            }

            // 所有工具调用都归类为CODE_GENERATION，不再单独分类
            if (stackTrace.contains("AiImageSearchTool") || stackTrace.contains("searchContentImages") ||
                    stackTrace.contains("FileWriteTool") || stackTrace.contains("FileReadTool") ||
                    stackTrace.contains("writeFile") || stackTrace.contains("readFile") ||
                    stackTrace.contains("MermaidTool") || stackTrace.contains("generateMermaid")) {
                return AiCallPurposeEnum.CODE_GENERATION.getCode();
            }

            // 保持原有用途
            return currentContext.getAiCallPurpose();

        } catch (Exception e) {
            log.warn("检测AI调用用途时发生异常，使用原有用途: {}", e.getMessage());
            return currentContext.getAiCallPurpose();
        }
    }

    /**
     * 获取调用栈信息用于检测调用源
     */
    private String getStackTraceInfo() {
        try {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < Math.min(stackTrace.length, 15); i++) {
                sb.append(stackTrace[i].getClassName())
                        .append(".")
                        .append(stackTrace[i].getMethodName())
                        .append(" ");
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    public void onResponse(ChatModelResponseContext responseContext) {
        log.info("监控 onResponse: {}", responseContext);

        // 获取请求ID
        String requestId = (String) responseContext.attributes().get("request_id");

        // 多种方式获取MonitorContext
        MonitorContext context = null;

        // 1. 尝试从请求ID获取
        if (requestId != null) {
            context = requestContextManager.getContext(requestId);
            if (context != null) {
                log.info("从请求ID获取MonitorContext成功: requestId={}, userId={}, appId={}, aiCallPurpose={}",
                        requestId, context.getUserId(), context.getAppId(), context.getAiCallPurpose());
            }
        }

        // 2. 尝试从attributes获取
        if (context == null) {
            context = (MonitorContext) responseContext.attributes().get(MONITOR_CONTEXT_KEY);
            if (context != null) {
                log.info("从responseContext.attributes()获取MonitorContext成功: userId={}, appId={}, aiCallPurpose={}",
                        context.getUserId(), context.getAppId(), context.getAiCallPurpose());
            }
        }

        // 3. 尝试从ThreadLocal获取
        if (context == null) {
            log.warn("从请求ID和attributes获取MonitorContext失败，尝试从ThreadLocal获取");
            context = MonitorContextHolder.getContext();
            if (context != null) {
                log.info("从ThreadLocal获取MonitorContext成功: userId={}, appId={}, aiCallPurpose={}",
                        context.getUserId(), context.getAppId(), context.getAiCallPurpose());
            }
        }

        if (context == null) {
            log.warn("MonitorContext为空，跳过Token统计");
            return;
        }

        log.info("成功获取MonitorContext: userId={}, appId={}, aiCallPurpose={}",
                context.getUserId(), context.getAppId(), context.getAiCallPurpose());

        // 验证上下文数据有效性
        if ("unknown".equals(context.getUserId()) || "unknown".equals(context.getAppId())) {
            log.warn("MonitorContext包含无效数据 userId={}, appId={}, 尝试从模型响应中提取信息",
                    context.getUserId(), context.getAppId());

            // 尝试兜底处理：使用system用户ID记录统计，至少不丢失token信息
            TokenUsage fallbackTokenUsage = new TokenUsage();
            // 系统用户ID
            fallbackTokenUsage.setUserId(0L);
            // 系统应用ID
            fallbackTokenUsage.setAppId(0L);
            fallbackTokenUsage.setModelName(responseContext.chatResponse().modelName());
            fallbackTokenUsage.setAppType("UNKNOWN");
            fallbackTokenUsage.setRequestTokenCount(responseContext.chatResponse().tokenUsage().inputTokenCount());
            fallbackTokenUsage.setCreateTime(LocalDateTime.now());
            fallbackTokenUsage.setResponseTokenCount(responseContext.chatResponse().tokenUsage().outputTokenCount());
            fallbackTokenUsage.setTotalTokenCount(responseContext.chatResponse().tokenUsage().totalTokenCount());

            try {
                tokenUsageService.save(fallbackTokenUsage);
                log.info("保存兜底Token统计成功: model={}, input={}, output={}, total={}",
                        responseContext.chatResponse().modelName(),
                        responseContext.chatResponse().tokenUsage().inputTokenCount(),
                        responseContext.chatResponse().tokenUsage().outputTokenCount(),
                        responseContext.chatResponse().tokenUsage().totalTokenCount());
            } catch (Exception e) {
                log.error("保存兜底Token统计失败: {}", e.getMessage());
            }

            // 清理请求ID上下文
            if (requestId != null) {
                requestContextManager.removeContext(requestId);
            }
            return;
        }

        try {
            Long appId = Long.valueOf(context.getAppId());
            Long userId = Long.valueOf(context.getUserId());
            String appType = getAppTypeSafely(context);

            Integer inputTokens = responseContext.chatResponse().tokenUsage().inputTokenCount();
            Integer outputTokens = responseContext.chatResponse().tokenUsage().outputTokenCount();
            Integer totalTokens = responseContext.chatResponse().tokenUsage().totalTokenCount();

            log.info("Token统计: userId={}, appId={}, aiCallPurpose={}, totalTokens={}",
                    context.getUserId(), context.getAppId(), appType, totalTokens);

            // 【Token累加合并】对于CODE_GENERATION类型，检查是否已存在记录并累加
            if ("CODE_GENERATION".equals(appType)) {
                TokenUsage existingRecord = tokenUsageService.findByAppIdAndPurpose(appId, appType);

                if (existingRecord != null) {

                    // 累加Token到已存在的记录
                    existingRecord.setRequestTokenCount(existingRecord.getRequestTokenCount() + inputTokens);
                    existingRecord.setResponseTokenCount(existingRecord.getResponseTokenCount() + outputTokens);
                    existingRecord.setTotalTokenCount(existingRecord.getTotalTokenCount() + totalTokens);
                    // 更新时间为最新
                    existingRecord.setCreateTime(LocalDateTime.now());

                    // 如果模型名称不同，用逗号分隔记录多个模型
                    if (!existingRecord.getModelName().contains(responseContext.chatResponse().modelName())) {
                        existingRecord.setModelName(
                                existingRecord.getModelName() + "," + responseContext.chatResponse().modelName());
                    }

                    tokenUsageService.updateById(existingRecord);

                    log.info("Token累加: appId={}, totalTokens={}",
                            context.getAppId(), existingRecord.getTotalTokenCount());
                } else {
                    // 首次创建CODE_GENERATION记录
                    TokenUsage tokenUsage = new TokenUsage();
                    tokenUsage.setUserId(userId);
                    tokenUsage.setAppId(appId);
                    tokenUsage.setModelName(responseContext.chatResponse().modelName());
                    tokenUsage.setAppType(appType);
                    tokenUsage.setRequestTokenCount(inputTokens);
                    tokenUsage.setResponseTokenCount(outputTokens);
                    tokenUsage.setTotalTokenCount(totalTokens);
                    tokenUsage.setCreateTime(LocalDateTime.now());

                    tokenUsageService.save(tokenUsage);

                    log.info("Token统计创建: appId={}, totalTokens={}", context.getAppId(), totalTokens);
                }
            } else {
                // 非CODE_GENERATION类型，正常保存（ROUTING, INPUT_SAFETY_CHECK等）
                TokenUsage tokenUsage = new TokenUsage();
                tokenUsage.setUserId(userId);
                tokenUsage.setAppId(appId);
                tokenUsage.setModelName(responseContext.chatResponse().modelName());
                tokenUsage.setAppType(appType);
                tokenUsage.setRequestTokenCount(inputTokens);
                tokenUsage.setResponseTokenCount(outputTokens);
                tokenUsage.setTotalTokenCount(totalTokens);
                tokenUsage.setCreateTime(LocalDateTime.now());

                tokenUsageService.save(tokenUsage);

                log.info("Token统计保存: appId={}, aiCallPurpose={}, totalTokens={}",
                        context.getAppId(), appType, totalTokens);
            }
        } catch (NumberFormatException e) {
            log.error("Token统计保存失败，ID格式错误: userId={}, appId={}, error={}",
                    context.getUserId(), context.getAppId(), e.getMessage());
        } catch (Exception e) {
            log.error("Token统计保存失败: userId={}, appId={}, error={}",
                    context.getUserId(), context.getAppId(), e.getMessage(), e);
        } finally {
            // 🔥 【修复】完整清理所有上下文

            // 1. 清理请求ID上下文
            if (requestId != null) {
                requestContextManager.removeContext(requestId);
            }

            // 2. 清理ThreadLocal上下文
            MonitorContextHolder.clearContext();

            // 3. 清理GlobalContextStorage中的特定上下文
            if (context != null && context.getAppId() != null) {
                GlobalContextStorage.removeContext(context.getAppId());
                log.debug("🧹 清理全局上下文: appId={}", context.getAppId());
            }

            log.debug("🧹 完整清理上下文完成: requestId={}", requestId);
        }
    }

    @Override
    public void onError(ChatModelErrorContext errorContext) {
        log.info("监控 onError: {}", errorContext);
        log.error("Error occurred: {}", errorContext.error().getMessage());

        // 🔥 【修复】错误情况下也要清理上下文
        try {
            // 获取请求ID
            String requestId = (String) errorContext.attributes().get("request_id");

            // 获取上下文
            MonitorContext context = null;
            if (requestId != null) {
                context = requestContextManager.getContext(requestId);
            }
            if (context == null) {
                context = MonitorContextHolder.getContext();
            }

            // 清理上下文
            if (requestId != null) {
                requestContextManager.removeContext(requestId);
            }
            MonitorContextHolder.clearContext();
            if (context != null && context.getAppId() != null) {
                GlobalContextStorage.removeContext(context.getAppId());
            }

            log.debug("🧹 错误情况下完整清理上下文完成: requestId={}", requestId);
        } catch (Exception e) {
            log.warn("清理错误上下文时发生异常: {}", e.getMessage());
        }
    }

    /**
     * 多种方式获取监控上下文
     */
    private MonitorContext getMonitorContext() {
        // 1. 尝试从ThreadLocal获取
        MonitorContext context = MonitorContextHolder.getContext();
        if (context != null) {
            log.debug("从ThreadLocal获取MonitorContext成功: userId={}, appId={}, aiCallPurpose={}",
                    context.getUserId(), context.getAppId(), context.getAiCallPurpose());
            return context;
        }

        log.warn("ThreadLocal中未找到MonitorContext，尝试从Reactor Context获取");

        // 2. 尝试从Reactor Context获取
        try {
            MonitorContext reactorContext = Mono.deferContextual(ctx -> {
                MonitorContext ctxFromReactor = MonitorContextHolder.getContextFromReactor(ctx);
                return Mono.justOrEmpty(ctxFromReactor);
                // 短暂超时，避免阻塞
            }).block(Duration.ofMillis(100));

            if (reactorContext != null) {
                log.info("从Reactor Context获取MonitorContext成功: userId={}, appId={}, aiCallPurpose={}",
                        reactorContext.getUserId(), reactorContext.getAppId(), reactorContext.getAiCallPurpose());
                return reactorContext;
            }
        } catch (Exception e) {
            log.warn("从Reactor Context获取MonitorContext失败: {}", e.getMessage());
        }

        log.warn("所有方式都无法获取MonitorContext");
        return null;
    }

    /**
     * 安全地获取AI调用用途，优先从MonitorContext获取，防止数据库查询异常
     */
    private String getAppTypeSafely(MonitorContext context) {
        String appId = context.getAppId();

        try {
            // 策略1: 优先从MonitorContext获取AI调用用途
            String aiCallPurpose = context.getAiCallPurpose();
            log.info("检查MonitorContext: appId={}, aiCallPurpose={}", appId, aiCallPurpose);

            if (aiCallPurpose != null && !aiCallPurpose.trim().isEmpty() && !"UNKNOWN".equals(aiCallPurpose)) {
                log.info("从MonitorContext获取AI调用用途: appId={}, aiCallPurpose={}", appId, aiCallPurpose);
                return aiCallPurpose;
            } else {
                log.warn("MonitorContext中的aiCallPurpose无效: appId={}, aiCallPurpose={}", appId, aiCallPurpose);
            }

            // 策略2: 从数据库获取（兜底方案）
            if (appId == null || "unknown".equals(appId)) {
                log.warn("appId无效，使用默认AI调用用途: {}", appId);
                return "UNKNOWN";
            }

            log.info("尝试从数据库获取应用信息: appId={}", appId);
            Long appIdLong = Long.valueOf(appId);
            var app = appService.getById(appIdLong);

            if (app == null) {
                log.warn("未找到应用记录，使用默认AI调用用途: appId={}", appId);
                return "UNKNOWN";
            }

            String dbCodeGenType = app.getCodeGenType();
            if (dbCodeGenType == null || dbCodeGenType.trim().isEmpty()) {
                log.warn("应用的codeGenType为空，使用默认值: appId={}", appId);
                return "UNKNOWN";
            }

            // 将数据库中的代码生成类型映射为AI调用用途
            String mappedPurpose = mapCodeGenTypeToAiCallPurpose(dbCodeGenType);
            log.info("从数据库获取并映射AI调用用途: appId={}, codeGenType={}, mappedPurpose={}",
                    appId, dbCodeGenType, mappedPurpose);
            return mappedPurpose;

        } catch (NumberFormatException e) {
            log.error("appId格式错误，使用默认AI调用用途: appId={}, error={}", appId, e.getMessage());
            return "UNKNOWN";
        } catch (Exception e) {
            log.error("获取AI调用用途失败，使用默认值: appId={}, error={}", appId, e.getMessage());
            return "UNKNOWN";
        }
    }

    /**
     * 将旧的代码生成类型映射为新的AI调用用途
     */
    private String mapCodeGenTypeToAiCallPurpose(String codeGenType) {
        if (codeGenType == null) {
            return "UNKNOWN";
        }

        return switch (codeGenType.toUpperCase()) {
            case "HTML", "VUE_PROJECT", "MULTI_FILE" -> "CODE_GENERATION";
            case "ROUTING" -> "ROUTING";
            // 默认认为是代码生成
            default -> "CODE_GENERATION";
        };
    }
}
