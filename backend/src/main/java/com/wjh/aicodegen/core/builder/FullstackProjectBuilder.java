package com.wjh.aicodegen.core.builder;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RuntimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
        log.info("开始构建全栈项目: {}", projectPath);

        // 1. 等待项目文件生成
        if (!waitForProjectFiles(projectPath, 60)) {
            log.error("等待项目文件超时: {}", projectPath);
            return false;
        }

        // 2. 执行 schema.sql
        String tablePrefix = "project_" + appId + "_";
        Path schemaPath = Paths.get(projectPath, "schema.sql");
        if (Files.exists(schemaPath)) {
            try {
                List<String> tables = sqlExecutor.executeSchema(schemaPath, tablePrefix, appId);
                log.info("Schema 执行成功，创建了 {} 个表: {}", tables.size(), tables);

                // 写入 .env 文件（注入真实数据库连接信息）
                writeEnvFile(projectPath, tablePrefix);
            } catch (Exception e) {
                log.error("Schema 执行失败: {}", e.getMessage(), e);
                return false;
            }
        } else {
            log.warn("未找到 schema.sql，跳过数据库初始化");
        }

        // 3. npm install + build 后端
        Path serverPath = Paths.get(projectPath, "server");
        if (Files.exists(serverPath)) {
            log.info("安装后端依赖...");
            if (!runNpmInstall(serverPath.toString())) {
                log.error("后端 npm install 失败");
                return false;
            }
        }

        // 4. npm install + build 前端
        Path frontendPath = Paths.get(projectPath, "frontend");
        if (Files.exists(frontendPath)) {
            log.info("安装前端依赖...");
            if (!runNpmInstall(frontendPath.toString())) {
                log.error("前端 npm install 失败");
                return false;
            }

            log.info("构建前端...");
            if (!runNpmBuild(frontendPath.toString())) {
                log.error("前端 npm run build 失败");
                return false;
            }

            // 5. 复制前端 dist 到 server/public
            Path distPath = frontendPath.resolve("dist");
            Path publicPath = serverPath.resolve("public");
            if (Files.exists(distPath)) {
                FileUtil.copyContent(distPath.toFile(), publicPath.toFile(), true);
                log.info("前端 dist 已复制到 server/public");
            }
        }

        log.info("全栈项目构建完成: {}", projectPath);
        return true;
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

            String envContent = String.format("""
                    DB_HOST=%s
                    DB_PORT=%s
                    DB_NAME=%s
                    DB_USER=%s
                    DB_PASS=%s
                    DB_TABLE_PREFIX=%s
                    """, host, port, dbName, dbUsername, dbPassword, tablePrefix);

            Path envPath = Paths.get(projectPath, ".env");
            Files.writeString(envPath, envContent);
            log.info(".env 文件已写入: {}", envPath);
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
        return runNpmCommand(dirPath, "install");
    }

    private boolean runNpmBuild(String dirPath) {
        return runNpmCommand(dirPath, "run build");
    }

    private boolean runNpmCommand(String dirPath, String command) {
        try {
            String npmCmd = System.getProperty("os.name").toLowerCase().contains("windows")
                    ? "npm.cmd" : "npm";
            String[] cmd = command.split(" ");
            String[] fullCmd = new String[cmd.length + 1];
            fullCmd[0] = npmCmd;
            System.arraycopy(cmd, 0, fullCmd, 1, cmd.length);

            ProcessBuilder pb = new ProcessBuilder(fullCmd);
            pb.directory(new File(dirPath));
            pb.redirectErrorStream(true);

            Process process = pb.start();
            boolean finished = process.waitFor(5, java.util.concurrent.TimeUnit.MINUTES);

            if (!finished) {
                process.destroyForcibly();
                log.error("npm {} 超时: {}", command, dirPath);
                return false;
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                String output = new String(process.getInputStream().readAllBytes());
                log.error("npm {} 失败 (exit={}): {}", command, exitCode, output.substring(0, Math.min(500, output.length())));
                return false;
            }

            return true;
        } catch (Exception e) {
            log.error("npm {} 异常: {}", command, e.getMessage());
            return false;
        }
    }
}
