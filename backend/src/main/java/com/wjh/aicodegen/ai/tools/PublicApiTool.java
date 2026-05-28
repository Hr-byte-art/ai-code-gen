package com.wjh.aicodegen.ai.tools;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 公共 API 工具基类
 * 提供统一的 HTTP 请求、缓存、错误处理能力
 */
@Slf4j
public abstract class PublicApiTool extends BaseTool {

    private static final int DEFAULT_TIMEOUT_MS = 5000;

    /** 简单内存缓存：key -> CachedEntry */
    private final Map<String, CachedEntry> cache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 5 * 60 * 1000; // 5 分钟

    /**
     * 发送 GET 请求
     *
     * @param url 请求 URL
     * @return 响应体字符串，失败返回 null
     */
    protected String doGet(String url) {
        return doGet(url, DEFAULT_TIMEOUT_MS);
    }

    /**
     * 发送带自定义超时的 GET 请求
     */
    protected String doGet(String url, int timeoutMs) {
        try {
            HttpResponse response = HttpRequest.get(url)
                    .timeout(timeoutMs)
                    .header("User-Agent", "AiCodeGenPlatform/1.0")
                    .execute();

            if (response.isOk()) {
                return response.body();
            } else {
                log.warn("HTTP GET 失败: url={}, status={}", url, response.getStatus());
                return null;
            }
        } catch (Exception e) {
            log.error("HTTP GET 异常: url={}, error={}", url, e.getMessage());
            return null;
        }
    }

    /**
     * 带缓存的 GET 请求
     */
    protected String doGetWithCache(String url) {
        return doGetWithCache(url, CACHE_TTL_MS);
    }

    protected String doGetWithCache(String url, long cacheTtlMs) {
        CachedEntry cached = cache.get(url);
        if (cached != null && !cached.isExpired(cacheTtlMs)) {
            log.debug("命中缓存: {}", url);
            return cached.value;
        }

        String result = doGet(url);
        if (result != null) {
            cache.put(url, new CachedEntry(result));
        }
        return result;
    }

    /**
     * 清理过期缓存
     */
    protected void cleanExpiredCache() {
        cache.entrySet().removeIf(entry -> entry.getValue().isExpired(CACHE_TTL_MS));
    }

    private static class CachedEntry {
        final String value;
        final long createdAt;

        CachedEntry(String value) {
            this.value = value;
            this.createdAt = System.currentTimeMillis();
        }

        boolean isExpired(long ttlMs) {
            return System.currentTimeMillis() - createdAt > ttlMs;
        }
    }
}
