package com.wjh.aicodegen.utils;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

/**
 * 缓存工具类
 * 用于手动清除缓存
 *
 * @author 王哈哈
 */
@Component
@Slf4j
public class CacheUtils {

    @Resource
    private CacheManager cacheManager;

    /**
     * 清除精选应用列表缓存
     */
    public void clearGoodAppListCache() {
        try {
            var cache = cacheManager.getCache("good_App_List_Cache");
            if (cache != null) {
                cache.clear();
                log.info("精选应用列表缓存已清除");
            }
        } catch (Exception e) {
            log.error("清除精选应用列表缓存失败", e);
        }
    }

    /**
     * 清除所有缓存
     */
    public void clearAllCache() {
        try {
            cacheManager.getCacheNames().forEach(cacheName -> {
                var cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    cache.clear();
                    log.info("缓存 {} 已清除", cacheName);
                }
            });
            log.info("所有缓存已清除");
        } catch (Exception e) {
            log.error("清除所有缓存失败", e);
        }
    }
}
