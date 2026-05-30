package com.wjh.aicodegen.core.deploy;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Node.js 进程管理器
 * 管理全栈项目 Express 后端的启动、停止和端口分配
 */
@Slf4j
@Component
public class NodeProcessManager {

    /** appId -> Process */
    private final ConcurrentHashMap<Long, Process> processes = new ConcurrentHashMap<>();

    /** appId -> port */
    private final ConcurrentHashMap<Long, Integer> portMap = new ConcurrentHashMap<>();

    /** 端口分配计数器，从 3001 开始 */
    private final AtomicInteger portCounter = new AtomicInteger(3001);

    /**
     * 启动 Express 服务
     *
     * @param appId     应用 ID
     * @param serverDir server 目录绝对路径
     * @return 分配的端口号，失败返回 -1
     */
    public int startServer(Long appId, String serverDir) {
        // 如果已经在运行，先停止
        if (isRunning(appId)) {
            log.info("Express 已在运行，先停止: appId={}", appId);
            stopServer(appId);
            // 等待端口释放
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
        }

        int port = allocatePort(appId);
        String nodeCmd = isWindows() ? "node.exe" : "node";

        try {
            ProcessBuilder pb = new ProcessBuilder(nodeCmd, "index.js");
            pb.directory(new File(serverDir));
            pb.redirectErrorStream(true);

            // 设置 PORT 环境变量
            pb.environment().put("PORT", String.valueOf(port));

            Process process = pb.start();
            processes.put(appId, process);

            // 等待端口就绪（最多 10 秒）
            boolean ready = waitForPortReady(port, 10);
            if (ready) {
                log.info("Express 启动成功: appId={}, port={}, dir={}", appId, port, serverDir);
                return port;
            } else {
                log.error("Express 启动超时: appId={}, port={}", appId, port);
                stopServer(appId);
                return -1;
            }
        } catch (Exception e) {
            log.error("Express 启动异常: appId={}, error={}", appId, e.getMessage());
            processes.remove(appId);
            return -1;
        }
    }

    /**
     * 停止 Express 服务
     */
    public void stopServer(Long appId) {
        Process process = processes.remove(appId);
        // 保留端口映射，重启时复用同一端口

        if (process == null) return;

        if (process.isAlive()) {
            process.destroy();
            try {
                boolean exited = process.waitFor(5, TimeUnit.SECONDS);
                if (!exited) {
                    process.destroyForcibly();
                    log.warn("Express 强制停止: appId={}", appId);
                } else {
                    log.info("Express 已停止: appId={}", appId);
                }
            } catch (InterruptedException e) {
                process.destroyForcibly();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 检查 Express 是否在运行
     */
    public boolean isRunning(Long appId) {
        Process process = processes.get(appId);
        return process != null && process.isAlive();
    }

    /**
     * 检查 Express 是否就绪（端口可连接）
     */
    public boolean isReady(Long appId) {
        int port = getPort(appId);
        if (port <= 0) return false;
        return isPortOpen(port);
    }

    /**
     * 获取应用的端口号
     */
    public int getPort(Long appId) {
        return portMap.getOrDefault(appId, -1);
    }

    /**
     * 获取 Express 服务的 URL
     */
    public String getUrl(Long appId) {
        int port = getPort(appId);
        if (port <= 0) return null;
        return "http://localhost:" + port;
    }

    /**
     * 等待端口就绪
     */
    private boolean waitForPortReady(int port, int timeoutSeconds) {
        for (int i = 0; i < timeoutSeconds * 2; i++) {
            if (isPortOpen(port)) return true;
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
        }
        return false;
    }

    /**
     * 检查端口是否可连接
     */
    private boolean isPortOpen(int port) {
        try (Socket socket = new Socket("localhost", port)) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 分配端口
     */
    private int allocatePort(Long appId) {
        // 如果已分配过端口，复用
        Integer existing = portMap.get(appId);
        if (existing != null) return existing;

        int port = portCounter.getAndIncrement();
        portMap.put(appId, port);
        return port;
    }

    private boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("windows");
    }

    /**
     * 应用关闭时停止所有 Node 进程
     */
    @PreDestroy
    public void shutdown() {
        log.info("停止所有 Express 进程...");
        for (Long appId : processes.keySet().toArray(new Long[0])) {
            stopServer(appId);
        }
        log.info("所有 Express 进程已停止");
    }
}
