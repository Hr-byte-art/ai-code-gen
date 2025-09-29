package com.wjh.aicodegen.monitor;

import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.output.TokenUsage;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * 增强的AI模型监控监听器
 * 支持多种上下文获取方式，解决多线程环境下的监控问题
 * 
 * @author 木子宸
 */
@Component("enhancedAiModelMonitorListener")
@Slf4j
public class EnhancedAiModelMonitorListener implements ChatModelListener {

    // 用于存储请求开始时间的键
    private static final String REQUEST_START_TIME_KEY = "request_start_time";
    // 用于监控上下文传递（因为请求和响应事件的触发不是同一个线程）
    private static final String MONITOR_CONTEXT_KEY = "monitor_context";

    @Resource
    private AiModelMetricsCollector aiModelMetricsCollector;

    @Override
    public void onRequest(ChatModelRequestContext requestContext) {
        // 记录请求开始时间
        requestContext.attributes().put(REQUEST_START_TIME_KEY, Instant.now());

        // 多种方式获取监控上下文
        MonitorContext context = getMonitorContext();
        if (context == null) {
            log.debug("MonitorContext not available in onRequest, creating default context");
            // 如果无法获取上下文，创建一个默认的（用于统计但不包含用户信息）
            context = MonitorContext.builder()
                    .userId("unknown")
                    .appId("unknown")
                    .build();
        }

        // 将上下文存储到请求属性中，确保跨线程传递
        requestContext.attributes().put(MONITOR_CONTEXT_KEY, context);

        // 记录请求指标
        String modelName = requestContext.chatRequest().modelName();
        aiModelMetricsCollector.recordRequest(context.getUserId(), context.getAppId(), modelName, "started");
    }

    @Override
    public void onResponse(ChatModelResponseContext responseContext) {
        // 从属性中获取监控信息（由 onRequest 方法存储）
        Map<Object, Object> attributes = responseContext.attributes();
        MonitorContext context = (MonitorContext) attributes.get(MONITOR_CONTEXT_KEY);

        if (context == null) {
            log.warn("MonitorContext is null in onResponse, skipping metrics recording");
            return;
        }

        String userId = context.getUserId();
        String appId = context.getAppId();
        String modelName = responseContext.chatResponse().modelName();

        // 记录成功请求
        aiModelMetricsCollector.recordRequest(userId, appId, modelName, "success");
        aiModelMetricsCollector.recordModelCallCount(userId, appId, modelName);

        // 记录响应时间
        recordResponseTime(attributes, userId, appId, modelName);

        // 记录 Token 使用情况
        recordTokenUsage(responseContext, userId, appId, modelName);
    }

    @Override
    public void onError(ChatModelErrorContext errorContext) {
        // 从属性中获取监控信息
        Map<Object, Object> attributes = errorContext.attributes();
        MonitorContext context = (MonitorContext) attributes.get(MONITOR_CONTEXT_KEY);

        if (context == null) {
            log.warn("MonitorContext is null in onError, skipping metrics recording");
            return;
        }

        String userId = context.getUserId();
        String appId = context.getAppId();
        String modelName = errorContext.chatRequest().modelName();
        String errorMessage = errorContext.error().getMessage();

        // 记录失败请求
        aiModelMetricsCollector.recordRequest(userId, appId, modelName, "error");
        aiModelMetricsCollector.recordError(userId, appId, modelName, errorMessage);

        // 记录响应时间（即使是错误响应）
        recordResponseTime(attributes, userId, appId, modelName);
    }

    /**
     * 多种方式获取监控上下文
     */
    private MonitorContext getMonitorContext() {
        // 1. 尝试从ThreadLocal获取
        MonitorContext context = MonitorContextHolder.getContext();
        if (context != null) {
            return context;
        }

        // 2. 尝试从Reactor Context获取
        try {
            return Mono.deferContextual(ctx -> {
                MonitorContext reactorContext = MonitorContextHolder.getContextFromReactor(ctx);
                return Mono.justOrEmpty(reactorContext);
            }).block(Duration.ofMillis(100)); // 短暂超时，避免阻塞
        } catch (Exception e) {
            log.debug("Failed to get context from Reactor: {}", e.getMessage());
        }

        return null;
    }

    /**
     * 记录响应时间
     */
    private void recordResponseTime(Map<Object, Object> attributes, String userId, String appId, String modelName) {
        Instant startTime = (Instant) attributes.get(REQUEST_START_TIME_KEY);
        if (startTime != null) {
            Duration responseTime = Duration.between(startTime, Instant.now());
            aiModelMetricsCollector.recordResponseTime(userId, appId, modelName, responseTime);
        } else {
            log.debug("Request start time is null, skipping response time recording");
        }
    }

    /**
     * 记录Token使用情况
     */
    private void recordTokenUsage(ChatModelResponseContext responseContext, String userId, String appId,
            String modelName) {
        TokenUsage tokenUsage = responseContext.chatResponse().metadata().tokenUsage();
        if (tokenUsage != null) {
            aiModelMetricsCollector.recordTokenUsage(userId, appId, modelName, "input", tokenUsage.inputTokenCount());
            aiModelMetricsCollector.recordTokenUsage(userId, appId, modelName, "output", tokenUsage.outputTokenCount());
            aiModelMetricsCollector.recordTokenUsage(userId, appId, modelName, "total", tokenUsage.totalTokenCount());
        }
    }
}
