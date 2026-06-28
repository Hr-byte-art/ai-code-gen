package com.wjh.aicodegen.core.deploy;

import cn.hutool.core.util.StrUtil;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
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

    /** appId -> 部署锁 */
    private final ConcurrentHashMap<Long, Object> deployLocks = new ConcurrentHashMap<>();

    /** appId -> port */
    private final ConcurrentHashMap<Long, Integer> portMap = new ConcurrentHashMap<>();

    /** appId -> 最近一次启动失败原因 */
    private final ConcurrentHashMap<Long, String> lastStartErrors = new ConcurrentHashMap<>();

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
        synchronized (deployLocks.computeIfAbsent(appId, key -> new Object())) {
            return doStartServer(appId, serverDir);
        }
    }

    private int doStartServer(Long appId, String serverDir) {
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
            File serverDirectory = new File(serverDir);
            if (!ensureDependenciesInstalled(serverDirectory)) {
                String errorMessage = "Express 依赖安装失败，请检查 server/package.json 和 npm install 输出";
                lastStartErrors.put(appId, errorMessage);
                log.error("Express 依赖安装失败: appId={}, dir={}", appId, serverDir);
                return -1;
            }

            ProcessBuilder pb = new ProcessBuilder(nodeCmd, "index.js");
            pb.directory(serverDirectory);
            pb.redirectErrorStream(true);
            File logDir = new File(serverDirectory, "logs");
            if (!logDir.exists() && !logDir.mkdirs()) {
                log.warn("Express 日志目录创建失败: {}", logDir.getAbsolutePath());
            }
            pb.redirectOutput(ProcessBuilder.Redirect.appendTo(new File(logDir, "express.log")));

            // 设置 PORT 环境变量
            pb.environment().put("PORT", String.valueOf(port));
            loadEnvFile(serverDirectory, pb);

            Process process = pb.start();
            processes.put(appId, process);

            // 等待 HTTP 服务就绪（最多 10 秒）
            boolean ready = waitForHttpReady(port, 10);
            if (ready) {
                lastStartErrors.remove(appId);
                log.info("Express 启动成功: appId={}, port={}, dir={}", appId, port, serverDir);
                return port;
            } else {
                String recentLog = readRecentExpressLog(serverDirectory);
                lastStartErrors.put(appId, StrUtil.blankToDefault(recentLog, "Express 启动超时，且未产生启动日志"));
                log.error("Express 启动超时: appId={}, port={}, recentLog={}", appId, port, recentLog);
                stopServer(appId);
                return -1;
            }
        } catch (Exception e) {
            lastStartErrors.put(appId, e.getMessage());
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
        return isHttpReady(port);
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
        return "http://127.0.0.1:" + port;
    }

    /**
     * 获取最近一次启动失败原因
     */
    public String getLastStartError(Long appId) {
        return lastStartErrors.getOrDefault(appId, "");
    }

    /**
     * 等待 HTTP 服务就绪
     */
    private boolean waitForHttpReady(int port, int timeoutSeconds) {
        for (int i = 0; i < timeoutSeconds * 2; i++) {
            if (isHttpReady(port)) return true;
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
        }
        return false;
    }

    /**
     * 检查 HTTP 服务是否可响应
     */
    private boolean isHttpReady(int port) {
        return isHttpReady(port, "/api/health") || isHttpReady(port, "/");
    }

    private boolean isHttpReady(int port, String path) {
        HttpURLConnection connection = null;
        try {
            URI uri = URI.create("http://127.0.0.1:" + port + path);
            connection = (HttpURLConnection) uri.toURL().openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(1000);
            connection.setReadTimeout(2000);
            int status = connection.getResponseCode();
            return status >= 200 && status < 500;
        } catch (Exception e) {
            return false;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private void loadEnvFile(File serverDirectory, ProcessBuilder processBuilder) {
        File envFile = new File(serverDirectory, ".env");
        if (!envFile.exists() && serverDirectory.getParentFile() != null) {
            envFile = new File(serverDirectory.getParentFile(), ".env");
        }
        if (!envFile.exists() || !envFile.isFile()) {
            log.info("未找到 Express .env 文件，使用当前进程环境变量: dir={}", serverDirectory.getAbsolutePath());
            return;
        }
        try {
            List<String> lines = Files.readAllLines(envFile.toPath());
            int loaded = 0;
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isBlank() || trimmed.startsWith("#") || !trimmed.contains("=")) {
                    continue;
                }
                int separator = trimmed.indexOf('=');
                String key = trimmed.substring(0, separator).trim();
                String value = trimmed.substring(separator + 1).trim();
                if (key.isBlank()) {
                    continue;
                }
                processBuilder.environment().put(key, value);
                loaded++;
            }
            log.info("已加载 Express 环境变量: file={}, count={}", envFile.getAbsolutePath(), loaded);
        } catch (Exception e) {
            log.warn("加载 Express .env 文件失败: file={}, error={}", envFile.getAbsolutePath(), e.getMessage());
        }
    }

    private boolean ensureDependenciesInstalled(File serverDirectory) {
        File packageJson = new File(serverDirectory, "package.json");
        if (!packageJson.exists() || !packageJson.isFile()) {
            return true;
        }
        if (hasExpressDependency(serverDirectory)) {
            return true;
        }

        String npmCmd = isWindows() ? "npm.cmd" : "npm";
        long startMs = System.currentTimeMillis();
        try {
            log.info("Express 依赖缺失，执行 npm install: dir={}", serverDirectory.getAbsolutePath());
            ProcessBuilder pb = new ProcessBuilder(npmCmd, "install");
            pb.directory(serverDirectory);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Thread outputReader = Thread.ofVirtual().name("express-npm-install-" + System.currentTimeMillis()).start(() -> {
                try (var inputStream = process.getInputStream()) {
                    inputStream.transferTo(outputStream);
                } catch (Exception e) {
                    log.warn("读取 Express npm install 输出失败: {}", e.getMessage());
                }
            });

            boolean finished = process.waitFor(300, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                outputReader.join(TimeUnit.SECONDS.toMillis(5));
                log.error("Express npm install 超时: dir={}, output={}", serverDirectory.getAbsolutePath(), limitOutput(outputStream));
                return false;
            }

            outputReader.join(TimeUnit.SECONDS.toMillis(5));
            int exitCode = process.exitValue();
            if (exitCode != 0) {
                log.error("Express npm install 失败: dir={}, exit={}, output={}",
                        serverDirectory.getAbsolutePath(), exitCode, limitOutput(outputStream));
                return false;
            }

            log.info("Express npm install 成功: dir={}, durationMs={}",
                    serverDirectory.getAbsolutePath(), System.currentTimeMillis() - startMs);
            return hasExpressDependency(serverDirectory);
        } catch (Exception e) {
            log.error("Express npm install 异常: dir={}, error={}", serverDirectory.getAbsolutePath(), e.getMessage());
            return false;
        }
    }

    private boolean hasExpressDependency(File serverDirectory) {
        return new File(serverDirectory, "node_modules/express/package.json").exists();
    }

    private String readRecentExpressLog(File serverDirectory) {
        File logFile = new File(new File(serverDirectory, "logs"), "express.log");
        if (!logFile.exists() || !logFile.isFile()) {
            return "";
        }
        try {
            String content = Files.readString(logFile.toPath());
            return limitOutput(content);
        } catch (Exception e) {
            return "读取 Express 日志失败: " + e.getMessage();
        }
    }

    private String limitOutput(ByteArrayOutputStream outputStream) {
        return limitOutput(outputStream.toString(StandardCharsets.UTF_8));
    }

    private String limitOutput(String output) {
        if (output == null || output.isBlank()) {
            return "";
        }
        int maxLength = 4000;
        if (output.length() <= maxLength) {
            return output;
        }
        return output.substring(output.length() - maxLength);
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
