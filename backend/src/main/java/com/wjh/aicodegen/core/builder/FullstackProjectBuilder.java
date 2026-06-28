package com.wjh.aicodegen.core.builder;

import cn.hutool.core.io.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.wjh.aicodegen.core.deploy.NodeProcessManager;
import jakarta.annotation.Resource;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.locks.ReentrantLock;
import java.util.List;

/**
 * 全栈项目构建器
 * 负责：执行 schema.sql → npm install → npm run build → 复制前端到 server/public
 *
 * @author 王哈哈
 */
@Slf4j
@Component
public class FullstackProjectBuilder {

    @Resource
    private SqlExecutor sqlExecutor;

    @Resource
    private NodeProcessManager nodeProcessManager;

    @Value("${spring.datasource.username:root}")
    private String dbUsername;

    @Value("${spring.datasource.password:}")
    private String dbPassword;

    @Value("${spring.datasource.url:jdbc:mysql://localhost:3306/ai_code_gen}")
    private String dbUrl;

    /**
     * 异步构建全栈项目
     */
    public void buildProjectAsync(String projectPath, Long appId) {
        Thread.ofVirtual().name("fullstack-build-" + appId).start(() -> {
            try {
                buildProject(projectPath, appId);
            } catch (Exception e) {
                log.error("全栈项目构建失败: appId={}, error={}", appId, e.getMessage(), e);
            }
        });
    }

    /**
     * 构建全栈项目（同步）
     */
    public boolean buildProject(String projectPath, Long appId) {
        return buildProjectWithResult(projectPath, appId).isSuccess();
    }

    /**
     * 构建全栈项目并返回详细结果
     */
    public BuildResult buildProjectWithResult(String projectPath, Long appId) {
        ReentrantLock buildLock = BuildPathLockManager.getLock(projectPath);
        buildLock.lock();
        try {
            log.info("开始构建全栈项目: {}", projectPath);

            if (!waitForProjectFiles(projectPath, 60)) {
                log.error("等待项目文件超时: {}", projectPath);
                return BuildResult.failed("validate fullstack files", null, "", 0,
                        "等待项目文件超时: " + projectPath);
            }

            String tablePrefix = "project_" + appId + "_";
            Path schemaPath = Paths.get(projectPath, "schema.sql");
            if (Files.exists(schemaPath)) {
                try {
                    List<String> tables = sqlExecutor.executeSchema(schemaPath, tablePrefix, appId);
                    log.info("Schema 执行成功，创建了 {} 个表: {}", tables.size(), tables);
                    writeEnvFile(projectPath, tablePrefix);
                } catch (Exception e) {
                    log.error("Schema 执行失败: {}", e.getMessage(), e);
                    return BuildResult.failed("execute schema.sql", null, "", 0,
                            "Schema 执行失败: " + e.getMessage());
                }
            } else {
                log.warn("未找到 schema.sql，跳过数据库初始化");
            }

            Path serverPath = Paths.get(projectPath, "server");
            if (Files.exists(serverPath)) {
                log.info("安装后端依赖...");
                BuildResult serverInstallResult = runNpmInstallWithResult(serverPath.toString());
                if (!serverInstallResult.isSuccess()) {
                    log.error("后端 npm install 失败");
                    return serverInstallResult;
                }
            }

            Path frontendPath = Paths.get(projectPath, "frontend");
            BuildResult lastBuildResult = BuildResult.success("skip frontend build", 0, "", 0);
            if (Files.exists(frontendPath)) {
                log.info("安装前端依赖...");
                BuildResult frontendInstallResult = runNpmInstallWithResult(frontendPath.toString());
                if (!frontendInstallResult.isSuccess()) {
                    log.error("前端 npm install 失败");
                    return frontendInstallResult;
                }

                log.info("构建前端...");
                BuildResult buildResult = runNpmBuildWithResult(frontendPath.toString());
                if (!buildResult.isSuccess() && shouldRecoverNodeModules(buildResult)) {
                    log.warn("检测到全栈前端依赖疑似损坏，清理后重试一次: {}", frontendPath);
                    cleanNodeModules(frontendPath.toFile());
                    BuildResult retryInstallResult = runNpmInstallWithResult(frontendPath.toString());
                    if (!retryInstallResult.isSuccess()) {
                        log.error("清理后重新安装全栈前端依赖失败");
                        return retryInstallResult;
                    }
                    buildResult = runNpmBuildWithResult(frontendPath.toString());
                }
                lastBuildResult = buildResult;
                if (!lastBuildResult.isSuccess()) {
                    log.error("前端 npm run build 失败");
                    return lastBuildResult;
                }

                Path distPath = frontendPath.resolve("dist");
                Path publicPath = serverPath.resolve("public");
                if (Files.exists(distPath)) {
                    FileUtil.copyContent(distPath.toFile(), publicPath.toFile(), true);
                    log.info("前端 dist 已复制到 server/public");
                } else {
                    return BuildResult.failed("validate frontend dist", 0, lastBuildResult.getOutput(),
                            lastBuildResult.getDurationMs(), "前端构建完成但 dist 目录未生成: " + distPath);
                }
            }

            log.info("全栈项目构建完成: {}", projectPath);

            if (Files.exists(serverPath)) {
                int port = nodeProcessManager.startServer(appId, serverPath.toString());
                if (port > 0) {
                    log.info("Express 服务已启动: appId={}, port={}", appId, port);
                } else {
                    log.error("Express 服务启动失败: appId={}", appId);
                    return BuildResult.failed("start express server", null, lastBuildResult.getOutput(),
                            lastBuildResult.getDurationMs(), "Express 服务启动失败");
                }
            }

            return lastBuildResult;
        } finally {
            buildLock.unlock();
        }
    }

    /**
     * 写入 .env 文件（注入真实数据库连接信息）
     */
    private void writeEnvFile(String projectPath, String tablePrefix) {
        try {
            // 解析 JDBC URL
            String host = "localhost";
            String port = "3306";
            String dbName = "ai_code_gen";

            String url = dbUrl;
            // jdbc:mysql://host:port/dbname
            if (url.contains("//")) {
                String afterProtocol = url.substring(url.indexOf("//") + 2);
                if (afterProtocol.contains(":")) {
                    host = afterProtocol.substring(0, afterProtocol.indexOf(":"));
                    String afterHost = afterProtocol.substring(afterProtocol.indexOf(":") + 1);
                    if (afterHost.contains("/")) {
                        port = afterHost.substring(0, afterHost.indexOf("/"));
                        dbName = afterHost.substring(afterHost.indexOf("/") + 1);
                        // 去掉查询参数
                        if (dbName.contains("?")) {
                            dbName = dbName.substring(0, dbName.indexOf("?"));
                        }
                    }
                }
            }

            String envContent = String.format(
                    "DB_HOST=%s\n" +
                    "DB_PORT=%s\n" +
                    "DB_NAME=%s\n" +
                    "DB_USER=%s\n" +
                    "DB_PASS=%s\n" +
                    "DB_TABLE_PREFIX=%s\n",
                    host, port, dbName, dbUsername, dbPassword, tablePrefix);

            Path envPath = Paths.get(projectPath, ".env");
            Files.write(envPath, envContent.getBytes());
            Path serverEnvPath = Paths.get(projectPath, "server", ".env");
            if (Files.exists(serverEnvPath.getParent())) {
                Files.write(serverEnvPath, envContent.getBytes());
            }
            log.info(".env 文件已写入: {}, {}", envPath, serverEnvPath);
        } catch (IOException e) {
            log.error("写入 .env 文件失败: {}", e.getMessage());
        }
    }

    private boolean waitForProjectFiles(String projectPath, int timeoutSeconds) {
        for (int i = 0; i < timeoutSeconds; i++) {
            File serverDir = new File(projectPath, "server");
            File frontendDir = new File(projectPath, "frontend");
            if (serverDir.exists() && frontendDir.exists()) {
                try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                return true;
            }
            try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
        }
        return false;
    }

    private boolean runNpmInstall(String dirPath) {
        return runNpmInstallWithResult(dirPath).isSuccess();
    }

    private boolean runNpmBuild(String dirPath) {
        return runNpmBuildWithResult(dirPath).isSuccess();
    }

    private BuildResult runNpmInstallWithResult(String dirPath) {
        return runNpmCommand(dirPath, new String[]{getNpmCommand(), "install"}, 300);
    }

    private BuildResult runNpmBuildWithResult(String dirPath) {
        return runNpmCommand(dirPath, new String[]{getNpmCommand(), "run", "build"}, 300);
    }

    private String getNpmCommand() {
        return System.getProperty("os.name").toLowerCase().contains("windows") ? "npm.cmd" : "npm";
    }

    private BuildResult runNpmCommand(String dirPath, String[] command, int timeoutSeconds) {
        String commandText = String.join(" ", command);
        long startMs = System.currentTimeMillis();
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(new File(dirPath));
            pb.redirectErrorStream(true);

            Process process = pb.start();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Thread outputReader = Thread.ofVirtual().name("fullstack-build-output-" + System.currentTimeMillis()).start(() -> {
                try (var inputStream = process.getInputStream()) {
                    inputStream.transferTo(outputStream);
                } catch (IOException e) {
                    log.warn("读取构建输出失败: {}", e.getMessage());
                }
            });

            boolean finished = process.waitFor(timeoutSeconds, java.util.concurrent.TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                outputReader.join(java.util.concurrent.TimeUnit.SECONDS.toMillis(5));
                String output = outputStream.toString(StandardCharsets.UTF_8);
                log.error("npm 命令超时: {}, dir={}", commandText, dirPath);
                return BuildResult.failed(commandText, null, limitOutput(output),
                        System.currentTimeMillis() - startMs, "npm 命令执行超时");
            }

            outputReader.join(java.util.concurrent.TimeUnit.SECONDS.toMillis(5));
            int exitCode = process.exitValue();
            String output = outputStream.toString(StandardCharsets.UTF_8);
            long durationMs = System.currentTimeMillis() - startMs;
            if (exitCode != 0) {
                log.error("npm 命令失败 (exit={}): {}", exitCode, limitOutput(output));
                return BuildResult.failed(commandText, exitCode, limitOutput(output), durationMs, "npm 命令退出码非 0");
            }

            return BuildResult.success(commandText, exitCode, limitOutput(output), durationMs);
        } catch (Exception e) {
            log.error("npm 命令异常: {}, error={}", commandText, e.getMessage());
            return BuildResult.failed(commandText, null, "", System.currentTimeMillis() - startMs, e.getMessage());
        }
    }

    private boolean shouldRecoverNodeModules(BuildResult buildResult) {
        String output = buildResult == null ? "" : buildResult.getOutput();
        String errorMessage = buildResult == null ? "" : buildResult.getErrorMessage();
        String combined = (output + "\n" + errorMessage).toLowerCase();
        return combined.contains("err_module_not_found")
                || combined.contains("cannot find package")
                || combined.contains("esbuild")
                || combined.contains("vite build");
    }

    private void cleanNodeModules(File projectDir) {
        File nodeModulesDir = new File(projectDir, "node_modules");
        File packageLock = new File(projectDir, "package-lock.json");
        FileUtil.del(nodeModulesDir);
        if (packageLock.exists()) {
            FileUtil.del(packageLock);
        }
    }

    private String limitOutput(String output) {
        if (output == null) {
            return "";
        }
        int maxLength = 12000;
        if (output.length() <= maxLength) {
            return output;
        }
        return output.substring(output.length() - maxLength);
    }
}
