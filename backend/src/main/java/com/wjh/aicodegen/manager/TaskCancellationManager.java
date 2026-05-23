package com.wjh.aicodegen.manager;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import org.reactivestreams.Subscription;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 任务取消管理器
 * 用于管理正在进行的代码生成任务，支持任务取消
 * 
 * @author 王哈哈
 */
@Component
@Slf4j
public class TaskCancellationManager {

    /**
     * 存储正在进行的任务
     * Key: appId，Value: 任务的 Disposable 对象
     */
    private final ConcurrentMap<Long, Object> runningTasks = new ConcurrentHashMap<>();
    
    /**
     * 存储已取消的任务标记
     * Key: appId，Value: 取消时间戳
     */
    private final ConcurrentMap<Long, Long> cancelledTasks = new ConcurrentHashMap<>();

    /**
     * 注册正在运行的任务 (支持 Disposable)
     *
     * @param appId 应用ID
     * @param disposable 任务的 Disposable 对象
     */
    public void registerTask(Long appId, Disposable disposable) {
        if (appId != null && disposable != null) {
            // 清除之前的取消标记（如果有）
            cancelledTasks.remove(appId);
            // 如果已有任务在运行，先取消
            Object existingTask = runningTasks.put(appId, disposable);
            cancelExistingTask(existingTask, appId);
            log.debug("注册应用 {} 的代码生成任务", appId);
        }
    }

    /**
     * 注册正在运行的任务 (支持 Subscription)
     *
     * @param appId 应用ID
     * @param subscription 任务的 Subscription 对象
     */
    public void registerTask(Long appId, Subscription subscription) {
        if (appId != null && subscription != null) {
            // 清除之前的取消标记（如果有）
            cancelledTasks.remove(appId);
            // 如果已有任务在运行，先取消
            Object existingTask = runningTasks.put(appId, subscription);
            cancelExistingTask(existingTask, appId);
            log.debug("注册应用 {} 的代码生成任务", appId);
        }
    }

    /**
     * 取消现有任务的辅助方法
     */
    private void cancelExistingTask(Object existingTask, Long appId) {
        if (existingTask != null) {
            if (existingTask instanceof Disposable disposable && !disposable.isDisposed()) {
                log.info("应用 {} 已有任务在运行，取消旧任务 (Disposable)", appId);
                disposable.dispose();
            } else if (existingTask instanceof Subscription subscription) {
                log.info("应用 {} 已有任务在运行，取消旧任务 (Subscription)", appId);
                subscription.cancel();
            }
        }
    }

    /**
     * 取消指定应用的任务
     *
     * @param appId 应用ID
     * @return 是否成功取消
     */
    public boolean cancelTask(Long appId) {
        if (appId == null) {
            return false;
        }

        // 添加取消标记
        cancelledTasks.put(appId, System.currentTimeMillis());

        Object task = runningTasks.remove(appId);
        if (task != null) {
            if (task instanceof Disposable disposable && !disposable.isDisposed()) {
                disposable.dispose();
                log.info("成功取消应用 {} 的代码生成任务", appId);
                return true;
            } else if (task instanceof Subscription subscription) {
                subscription.cancel();
                log.info("成功取消应用 {} 的代码生成任务 (Subscription)，已调用 subscription.cancel()", appId);
                return true;
            }
        }
        log.debug("应用 {} 当前无活跃任务，已标记为取消状态", appId);
        return false;
    }

    /**
     * 任务完成时移除注册
     *
     * @param appId 应用ID
     */
    public void unregisterTask(Long appId) {
        if (appId != null) {
            runningTasks.remove(appId);
            // 清理取消标记（任务正常完成或异常结束）
            cancelledTasks.remove(appId);
            log.debug("移除应用 {} 的任务注册", appId);
        }
    }

    /**
     * 检查指定应用是否有正在运行的任务
     *
     * @param appId 应用ID
     * @return 是否有正在运行的任务
     */
    public boolean hasRunningTask(Long appId) {
        if (appId == null) {
            return false;
        }
        Object task = runningTasks.get(appId);
        if (task instanceof Disposable disposable) {
            return !disposable.isDisposed();
        } else if (task instanceof Subscription) {
            return true; // Subscription 没有简单的检查方法，假设存在即为运行中
        }
        return false;
    }

    /**
     * 检查指定应用的任务是否被取消
     *
     * @param appId 应用ID
     * @return 是否被取消
     */
    public boolean isTaskCancelled(Long appId) {
        if (appId == null) {
            return false;
        }
        return cancelledTasks.containsKey(appId);
    }

    /**
     * 获取当前正在运行的任务数量
     *
     * @return 正在运行的任务数量
     */
    public int getRunningTaskCount() {
        return (int) runningTasks.values().stream()
                .filter(task -> {
                    if (task instanceof Disposable disposable) {
                        return !disposable.isDisposed();
                    } else if (task instanceof Subscription) {
                        return true; // Subscription 假设存在即为运行中
                    }
                    return false;
                })
                .count();
    }
}
