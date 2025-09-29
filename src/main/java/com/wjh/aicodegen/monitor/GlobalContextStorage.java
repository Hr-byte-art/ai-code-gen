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
     * 获取当前存储的上下文数量
     * 
     * @return 上下文数量
     */
    public static int size() {
        return APP_CONTEXTS.size();
    }
}
