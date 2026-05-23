package com.wjh.aicodegen.monitor;

import lombok.extern.slf4j.Slf4j;
import reactor.util.context.Context;

/**
 * 监控上下文 (支持ThreadLocal和Reactor Context两种模式)
 * @author 王哈哈
 */
@Slf4j
public class MonitorContextHolder {

    private static final ThreadLocal<MonitorContext> CONTEXT_HOLDER = new ThreadLocal<>();
    
    // Reactor Context的键
    public static final String REACTOR_CONTEXT_KEY = "monitor_context";

    /**
     * 设置监控上下文（ThreadLocal模式）
     */
    public static void setContext(MonitorContext context) {
        CONTEXT_HOLDER.set(context);
    }

    /**
     * 获取当前监控上下文（ThreadLocal模式）
     */
    public static MonitorContext getContext() {
        return CONTEXT_HOLDER.get();
    }

    /**
     * 清除监控上下文（ThreadLocal模式）
     */
    public static void clearContext() {
        CONTEXT_HOLDER.remove();
    }
    
    /**
     * 从Reactor Context中获取监控上下文
     */
    public static MonitorContext getContextFromReactor(Context reactorContext) {
        return reactorContext.getOrDefault(REACTOR_CONTEXT_KEY, null);
    }
    
    /**
     * 从Reactor ContextView中获取监控上下文
     */
    public static MonitorContext getContextFromReactor(reactor.util.context.ContextView contextView) {
        return contextView.getOrDefault(REACTOR_CONTEXT_KEY, null);
    }
    
    /**
     * 创建包含监控上下文的Reactor Context
     */
    public static Context putContextToReactor(Context reactorContext, MonitorContext monitorContext) {
        return reactorContext.put(REACTOR_CONTEXT_KEY, monitorContext);
    }
}
