package com.wjh.aicodegen.manager;

import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 生成中对话流管理器。
 * 页面离开不会影响后台任务，新页面重新进入后可按 appId 继续订阅当前输出。
 */
@Slf4j
@Component
public class GenerationStreamManager {

    private static final int MAX_BUFFER_SIZE = 2000;

    private final ConcurrentMap<Long, GenerationStreamSession> sessions = new ConcurrentHashMap<>();

    public boolean hasActiveSession(Long appId) {
        GenerationStreamSession session = sessions.get(appId);
        return session != null && !session.finished;
    }

    public void start(Long appId) {
        GenerationStreamSession oldSession = sessions.remove(appId);
        if (oldSession != null) {
            oldSession.sink.tryEmitComplete();
        }
        sessions.put(appId, new GenerationStreamSession());
        log.info("应用 {} 生成流会话已创建", appId);
    }

    public void emit(Long appId, String chunk) {
        GenerationStreamSession session = sessions.get(appId);
        if (session == null || session.finished) {
            return;
        }
        synchronized (session.buffer) {
            session.buffer.add(chunk);
            if (session.buffer.size() > MAX_BUFFER_SIZE) {
                session.buffer.remove(0);
            }
        }
        session.sink.tryEmitNext(chunk);
    }

    public void error(Long appId, String message) {
        GenerationStreamSession session = sessions.get(appId);
        if (session == null || session.finished) {
            return;
        }
        String errorEvent = JSONUtil.toJsonStr(Map.of(
                "type", "biz_error",
                "message", message
        ));
        session.errorMessage = errorEvent;
        session.finished = true;
        synchronized (session.buffer) {
            session.buffer.add(errorEvent);
        }
        session.sink.tryEmitNext(errorEvent);
        session.sink.tryEmitComplete();
        log.info("应用 {} 生成流会话已失败", appId);
    }

    public void complete(Long appId) {
        GenerationStreamSession session = sessions.get(appId);
        if (session == null || session.finished) {
            return;
        }
        session.finished = true;
        session.sink.tryEmitComplete();
        log.info("应用 {} 生成流会话已完成", appId);
    }

    public Flux<String> subscribe(Long appId) {
        GenerationStreamSession session = sessions.get(appId);
        if (session == null) {
            return Flux.empty();
        }
        List<String> snapshot;
        synchronized (session.buffer) {
            snapshot = new ArrayList<>(session.buffer);
        }
        if (session.finished) {
            return Flux.fromIterable(snapshot);
        }
        return Flux.fromIterable(snapshot).concatWith(session.sink.asFlux());
    }

    public void remove(Long appId) {
        GenerationStreamSession session = sessions.remove(appId);
        if (session != null) {
            session.sink.tryEmitComplete();
            log.info("应用 {} 生成流会话已移除", appId);
        }
    }

    private static class GenerationStreamSession {
        private final Sinks.Many<String> sink = Sinks.many().multicast().onBackpressureBuffer();
        private final List<String> buffer = new ArrayList<>();
        private volatile boolean finished = false;
        private volatile String errorMessage;
    }
}