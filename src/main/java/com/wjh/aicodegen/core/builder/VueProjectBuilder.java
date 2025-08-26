package com.wjh.aicodegen.core.builder;

import cn.hutool.core.util.RuntimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.concurrent.TimeUnit;

/**
 * @Author 王哈哈
 * @Date 2025/8/13 00:40:19
 * @Description Vue项目构建器
 */
@Slf4j
@Component
public class VueProjectBuilder {

    /**
     * 异步构建项目（不阻塞主流程）
     *
     * @param projectPath 项目路径
     */
    public void buildProjectAsync(String projectPath) {
        // 在单独的线程中执行构建，避免阻塞主流程
        Thread.ofVirtual().name("vue-builder-" + System.currentTimeMillis()).start(() -> {
            try {
                // 等待文件生成完成，最多等待30秒
                if (waitForProjectFiles(projectPath, 30)) {
                    buildProject(projectPath);
                } else {
                    log.warn("等待项目文件生成超时，跳过构建: {}", projectPath);
                }
            } catch (Exception e) {
                log.error("异步构建 Vue 项目时发生异常: {}", e.getMessage(), e);
            }
        });
    }

    /**
     * 等待项目文件生成完成
     *
     * @param projectPath 项目路径
     * @param maxWaitSeconds 最大等待时间（秒）
     * @return 是否成功等待到文件生成
     */
    private boolean waitForProjectFiles(String projectPath, int maxWaitSeconds) {
        File projectDir = new File(projectPath);
        File packageJson = new File(projectDir, "package.json");
        
        int waitedSeconds = 0;
        while (waitedSeconds < maxWaitSeconds) {
            if (projectDir.exists() && packageJson.exists()) {
                log.info("检测到项目文件已生成: {}", projectPath);
                // 额外等待2秒确保所有文件写入完成
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
                return true;
            }
            
            try {
                Thread.sleep(1000); // 每秒检查一次
                waitedSeconds++;
                if (waitedSeconds % 5 == 0) {
                    log.info("等待项目文件生成中... ({}/{}秒)", waitedSeconds, maxWaitSeconds);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        
        log.error("等待项目文件生成超时: {}", projectPath);
        return false;
    }

    /**
     * 执行 npm install 命令
     */
    private boolean executeNpmInstall(File projectDir) {
        log.info("执行 npm install...");
        String command = String.format("%s install", buildCommand("npm"));
        // 5分钟超时
        return executeCommand(projectDir, command, 300);
    }

    /**
     * 执行 npm run build 命令
     */
    private boolean executeNpmBuild(File projectDir) {
        log.info("执行 npm run build...");
        String command = String.format("%s run build", buildCommand("npm"));
        // 3分钟超时
        return executeCommand(projectDir, command, 180);
    }


    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("windows");
    }

    private String buildCommand(String baseCommand) {
        if (isWindows()) {
            return baseCommand + ".cmd";
        }
        return baseCommand;
    }


    /**
     * 执行命令
     *
     * @param workingDir     工作目录
     * @param command        命令字符串
     * @param timeoutSeconds 超时时间（秒）
     * @return 是否执行成功
     */
    private boolean executeCommand(File workingDir, String command, int timeoutSeconds) {
        try {
            log.info("在目录 {} 中执行命令: {}", workingDir.getAbsolutePath(), command);
            Process process = RuntimeUtil.exec(
                    null,
                    workingDir,
                    // 命令分割为数组
                    command.split("\\s+")
            );
            // 等待进程完成，设置超时
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                log.error("命令执行超时（{}秒），强制终止进程", timeoutSeconds);
                process.destroyForcibly();
                return false;
            }
            int exitCode = process.exitValue();
            if (exitCode == 0) {
                log.info("命令执行成功: {}", command);
                return true;
            } else {
                log.error("命令执行失败，退出码: {}", exitCode);
                return false;
            }
        } catch (Exception e) {
            log.error("执行命令失败: {}, 错误信息: {}", command, e.getMessage());
            return false;
        }
    }


    /**
     * 构建 Vue 项目
     *
     * @param projectPath 项目根目录路径
     * @return 是否构建成功
     */
    public boolean buildProject(String projectPath) {
        File projectDir = new File(projectPath);
        if (!projectDir.exists() || !projectDir.isDirectory()) {
            log.error("项目目录不存在: {}", projectPath);
            return false;
        }
        // 检查 package.json 是否存在
        File packageJson = new File(projectDir, "package.json");
        if (!packageJson.exists()) {
            log.error("package.json 文件不存在: {}", packageJson.getAbsolutePath());
            log.error("项目目录内容: {}", listDirectoryContents(projectDir));
            return false;
        }
        log.info("开始构建 Vue 项目: {}", projectPath);
        log.info("项目目录结构: {}", listDirectoryContents(projectDir));
        // 执行 npm install
        if (!executeNpmInstall(projectDir)) {
            log.error("npm install 执行失败");
            return false;
        }
        // 执行 npm run build
        if (!executeNpmBuild(projectDir)) {
            log.error("npm run build 执行失败");
            return false;
        }
        // 验证 dist 目录是否生成
        File distDir = new File(projectDir, "dist");
        if (!distDir.exists()) {
            log.error("构建完成但 dist 目录未生成: {}", distDir.getAbsolutePath());
            return false;
        }
        log.info("Vue 项目构建成功，dist 目录: {}", distDir.getAbsolutePath());
        return true;
    }

    /**
     * 列出目录内容（用于调试）
     */
    private String listDirectoryContents(File dir) {
        if (!dir.exists() || !dir.isDirectory()) {
            return "目录不存在或不是目录";
        }
        
        File[] files = dir.listFiles();
        if (files == null || files.length == 0) {
            return "目录为空";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            sb.append(file.getName());
            if (file.isDirectory()) {
                sb.append("/");
            }
            if (i < files.length - 1) {
                sb.append(", ");
            }
        }
        sb.append("]");
        return sb.toString();
    }

}
