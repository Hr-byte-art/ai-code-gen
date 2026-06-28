package com.wjh.aicodegen.monitor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 基于请求ID的上下文管理器
 * 解决ThreadLocal和Reactor Context在异步流中丢失的问题
 */
@Slf4j
@Component
public class RequestContextManager {

    private final ConcurrentHashMap<String, MonitorContext> contextMap = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor();

    public RequestContextManager() {
        // 每5分钟清理一次过期的上下文
        cleanupExecutor.scheduleAtFixedRate(this::cleanupExpiredContexts, 5, 5, TimeUnit.MINUTES);
    }

    /**
     * 存储上下文到请求ID
     */
    public void storeContext(String requestId, MonitorContext context) {
        if (requestId != null && context != null) {
            contextMap.put(requestId, context);
            log.debug("存储上下文到请求ID: requestId={}, userId={}, appId={}, aiCallPurpose={}",
                    requestId, context.getUserId(), context.getAppId(), context.getAiCallPurpose());
        }
    }

    /**
     * 从请求ID获取上下文
     */
    public MonitorContext getContext(String requestId) {
        if (requestId == null) {
            return null;
        }

        MonitorContext context = contextMap.get(requestId);
        if (context != null) {
            log.debug("从请求ID获取上下文: requestId={}, userId={}, appId={}, aiCallPurpose={}",
                    requestId, context.getUserId(), context.getAppId(), context.getAiCallPurpose());
        } else {
            log.debug("未找到请求ID对应的上下文: requestId={}", requestId);
        }
        return context;
    }

    /**
     * 移除上下文
     */
    public void removeContext(String requestId) {
        if (requestId != null) {
            MonitorContext removed = contextMap.remove(requestId);
            if (removed != null) {
                log.debug("移除请求ID上下文: requestId={}", requestId);
            }
        }
    }

    /**
     * 清理过期的上下文（简单实现，实际项目中可以使用更复杂的过期策略）
     */
    private void cleanupExpiredContexts() {
        int sizeBefore = contextMap.size();
        // 这里可以实现更复杂的过期策略，比如基于时间戳
        // 目前只是简单的日志记录
        if (sizeBefore > 1000) {
            log.warn("上下文映射表过大: size={}, 建议检查是否有内存泄漏", sizeBefore);
        }
    }

    /**
     * 获取当前上下文数量（用于监控）
     */
    public int getContextCount() {
        return contextMap.size();
    }
}
