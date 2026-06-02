package com.wjh.aicodegen.core.builder;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 构建路径锁管理器
 * 用于串行化同一项目目录的构建流程，避免并发安装依赖或写入产物导致的损坏。
 */
public final class BuildPathLockManager {

    private static final ConcurrentHashMap<String, ReentrantLock> LOCKS = new ConcurrentHashMap<>();

    private BuildPathLockManager() {
    }

    public static ReentrantLock getLock(String path) {
        String normalizedPath = normalize(path);
        return LOCKS.computeIfAbsent(normalizedPath, ignored -> new ReentrantLock());
    }

    private static String normalize(String path) {
        if (path == null) {
            return "";
        }
        return path.replace('\\', '/').toLowerCase();
    }
}