package com.wjh.aicodegen.monitor;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 全局上下文存储
 * 用于解决工具调用时上下文丢失的问题
 * 
 * @author AI Assistant
 */
@Slf4j
public class GlobalContextStorage {

    /**
     * 全局最新上下文存储（按AppId）
     */
    private static final ConcurrentMap<String, MonitorContext> APP_CONTEXTS = new ConcurrentHashMap<>();

    /**
     * 最新的上下文（不区分AppId）
     */
    private static final AtomicReference<MonitorContext> LATEST_CONTEXT = new AtomicReference<>();

    /**
     * 存储上下文到全局存储
     *
     * @param context 监控上下文
     */
    public static void storeContext(MonitorContext context) {
        if (context != null && context.getAppId() != null) {
            APP_CONTEXTS.put(context.getAppId(), context);
            LATEST_CONTEXT.set(context);
            log.debug("存储上下文到全局存储: userId={}, appId={}, aiCallPurpose={}",
                    context.getUserId(), context.getAppId(), context.getAiCallPurpose());
            // 安全网：防止内存泄漏
            evictIfOverCapacity(100);
        }
    }

    /**
     * 根据AppId获取上下文
     * 
     * @param appId 应用ID
     * @return 监控上下文
     */
    public static MonitorContext getContext(String appId) {
        if (appId == null) {
            return null;
        }
        return APP_CONTEXTS.get(appId);
    }

    /**
     * 获取最新的上下文（不区分AppId）
     * 
     * @return 最新的监控上下文
     */
    public static MonitorContext getLatestContext() {
        return LATEST_CONTEXT.get();
    }

    /**
     * 清理指定AppId的上下文
     *
     * @param appId 应用ID
     */
    public static void removeContext(String appId) {
        if (appId != null) {
            MonitorContext removed = APP_CONTEXTS.remove(appId);
            if (removed != null) {
                log.debug("清理全局上下文: appId={}", appId);
            }
        }
        // 如果没有其他上下文，也清理 LATEST_CONTEXT
        if (APP_CONTEXTS.isEmpty()) {
            LATEST_CONTEXT.set(null);
        }
    }

    /**
     * 清理所有上下文
     */
    public static void clearAll() {
        APP_CONTEXTS.clear();
        LATEST_CONTEXT.set(null);
        log.debug("清理所有全局上下文");
    }

    /**
     * 安全网：限制最大上下文数量，防止内存泄漏
     * 当上下文数量超过限制时，清理最早的条目
     */
    public static void evictIfOverCapacity(int maxCapacity) {
        if (APP_CONTEXTS.size() > maxCapacity) {
            log.warn("全局上下文数量超过限制: size={}, max={}, 执行清理", APP_CONTEXTS.size(), maxCapacity);
            // 简单策略：清理一半
            int toRemove = APP_CONTEXTS.size() / 2;
            var iterator = APP_CONTEXTS.keySet().iterator();
            while (iterator.hasNext() && toRemove > 0) {
                iterator.next();
                iterator.remove();
                toRemove--;
            }
        }
    }

    /**
     * 获取当前存储的上下文数量
     * 
     * @return 上下文数量
     */
    public static int size() {
        return APP_CONTEXTS.size();
    }
}
