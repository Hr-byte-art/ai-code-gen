package com.wjh.aicodegen.manager;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 构建事件 Sinks 管理器
 * 管理每个 appId 的 SSE 事件流
 *
 * @author 王哈哈
 */
@Slf4j
@Component
public class BuildEventSinkManager {

    private final ConcurrentMap<Long, Sinks.Many<String>> sinks = new ConcurrentHashMap<>();

    /**
     * 获取或创建指定 appId 的事件流
     */
    public Flux<String> getOrCreateSink(Long appId) {
        Sinks.Many<String> sink = sinks.computeIfAbsent(appId,
                id -> Sinks.many().multicast().onBackpressureBuffer());
        return sink.asFlux();
    }

    /**
     * 发送事件到指定 appId 的流
     */
    public void emit(Long appId, String event) {
        Sinks.Many<String> sink = sinks.get(appId);
        if (sink != null) {
            sink.tryEmitNext(event);
            log.debug("发送构建事件到 appId={}: {}", appId, event);
        }
    }

    /**
     * 完成指定 appId 的事件流
     */
    public void complete(Long appId) {
        Sinks.Many<String> sink = sinks.remove(appId);
        if (sink != null) {
            sink.tryEmitComplete();
            log.debug("完成 appId={} 的构建事件流", appId);
        }
    }

    /**
     * 移除指定 appId 的 sink
     */
    public void remove(Long appId) {
        sinks.remove(appId);
    }

    /**
     * 获取当前活跃的 sink 数量
     */
    public int getActiveSinkCount() {
        return sinks.size();
    }
}
