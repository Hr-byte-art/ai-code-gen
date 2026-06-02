package com.wjh.aicodegen.core.builder;

import cn.hutool.core.io.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

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
    private BuildResult executeNpmInstall(File projectDir) {
        log.info("执行 npm install...");
        String[] command = {buildCommand("npm"), "install"};
        return executeCommand(projectDir, command, 300);
    }

    /**
     * 执行 npm run build 命令
     */
    private BuildResult executeNpmBuild(File projectDir) {
        log.info("执行 npm run build...");
        String[] command = {buildCommand("npm"), "run", "build", "--", "--base=./"};
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
     * @param command        命令数组
     * @param timeoutSeconds 超时时间（秒）
     * @return 构建结果
     */
    private BuildResult executeCommand(File workingDir, String[] command, int timeoutSeconds) {
        String commandText = String.join(" ", command);
        long startMs = System.currentTimeMillis();
        try {
            log.info("在目录 {} 中执行命令: {}", workingDir.getAbsolutePath(), commandText);
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.directory(workingDir);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Thread outputReader = Thread.ofVirtual().name("vue-build-output-" + System.currentTimeMillis()).start(() -> {
                try (var inputStream = process.getInputStream()) {
                    inputStream.transferTo(outputStream);
                } catch (IOException e) {
                    log.warn("读取构建输出失败: {}", e.getMessage());
                }
            });

            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                outputReader.join(TimeUnit.SECONDS.toMillis(5));
                String output = outputStream.toString(StandardCharsets.UTF_8);
                log.error("命令执行超时（{}秒），强制终止进程", timeoutSeconds);
                return BuildResult.failed(commandText, null, limitOutput(output),
                        System.currentTimeMillis() - startMs, "命令执行超时");
            }

            outputReader.join(TimeUnit.SECONDS.toMillis(5));
            int exitCode = process.exitValue();
            String output = outputStream.toString(StandardCharsets.UTF_8);
            long durationMs = System.currentTimeMillis() - startMs;
            if (exitCode == 0) {
                log.info("命令执行成功: {}", commandText);
                return BuildResult.success(commandText, exitCode, limitOutput(output), durationMs);
            }
            log.error("命令执行失败，退出码: {}, 输出: {}", exitCode, limitOutput(output));
            return BuildResult.failed(commandText, exitCode, limitOutput(output), durationMs, "命令退出码非 0");
        } catch (Exception e) {
            log.error("执行命令失败: {}, 错误信息: {}", commandText, e.getMessage());
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


    public boolean buildProject(String projectPath) {
        return buildProjectWithResult(projectPath).isSuccess();
    }

    /**
     * 构建 Vue 项目并返回详细结果
     *
     * @param projectPath 项目根目录路径
     * @return 构建结果
     */
    public BuildResult buildProjectWithResult(String projectPath) {
        ReentrantLock buildLock = BuildPathLockManager.getLock(projectPath);
        buildLock.lock();
        try {
            File projectDir = new File(projectPath);
            if (!projectDir.exists() || !projectDir.isDirectory()) {
                log.error("项目目录不存在: {}", projectPath);
                return BuildResult.failed("validate project", null, "", 0, "项目目录不存在: " + projectPath);
            }
            File packageJson = new File(projectDir, "package.json");
            if (!packageJson.exists()) {
                String directoryContents = listDirectoryContents(projectDir);
                log.error("package.json 文件不存在: {}", packageJson.getAbsolutePath());
                log.error("项目目录内容: {}", directoryContents);
                return BuildResult.failed("validate package.json", null, directoryContents, 0,
                        "package.json 文件不存在: " + packageJson.getAbsolutePath());
            }
            log.info("开始构建 Vue 项目: {}", projectPath);
            log.info("项目目录结构: {}", listDirectoryContents(projectDir));

            BuildResult installResult = executeNpmInstall(projectDir);
            if (!installResult.isSuccess()) {
                log.error("npm install 执行失败");
                return installResult;
            }

            BuildResult buildResult = executeNpmBuild(projectDir);
            if (!buildResult.isSuccess() && shouldRecoverNodeModules(buildResult)) {
                log.warn("检测到前端依赖疑似损坏，清理 node_modules 后重试一次: {}", projectPath);
                cleanNodeModules(projectDir);
                BuildResult retryInstallResult = executeNpmInstall(projectDir);
                if (!retryInstallResult.isSuccess()) {
                    log.error("清理后重新安装依赖失败");
                    return retryInstallResult;
                }
                buildResult = executeNpmBuild(projectDir);
            }
            if (!buildResult.isSuccess()) {
                log.error("npm run build 执行失败");
                return buildResult;
            }

            File distDir = new File(projectDir, "dist");
            if (!distDir.exists()) {
                log.error("构建完成但 dist 目录未生成: {}", distDir.getAbsolutePath());
                return BuildResult.failed("validate dist", 0, buildResult.getOutput(), buildResult.getDurationMs(),
                        "构建完成但 dist 目录未生成: " + distDir.getAbsolutePath());
            }
            log.info("Vue 项目构建成功，dist 目录: {}", distDir.getAbsolutePath());
            return buildResult;
        } finally {
            buildLock.unlock();
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
