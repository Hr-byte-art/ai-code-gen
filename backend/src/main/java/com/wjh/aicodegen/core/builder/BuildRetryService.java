package com.wjh.aicodegen.core.builder;

import cn.hutool.core.io.FileUtil;
import com.wjh.aicodegen.ai.factory.AiCodeGeneratorServiceFactory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.File;

/**
 * 构建重试服务
 * 构建失败时使用 AI 分析错误并修复代码，然后重试构建
 */
@Slf4j
@Service
public class BuildRetryService {

    @Resource
    private VueProjectBuilder vueProjectBuilder;

    @Resource
    private FullstackProjectBuilder fullstackProjectBuilder;

    @Resource
    @Lazy
    private AiCodeGeneratorServiceFactory aiCodeGeneratorServiceFactory;

    /** 最大重试次数 */
    private static final int MAX_BUILD_RETRIES = 2;
    private static final String INTERNAL_BUILD_FIX_MARKER = "## INTERNAL_BUILD_FIX_TASK";

    /**
     * 带重试的构建
     *
     * @param projectPath   项目路径
     * @param buildStrategy 构建策略
     * @param appId         应用ID
     * @return 构建结果
     */
    public BuildRetryResult buildWithRetry(String projectPath, String buildStrategy, Long appId) {
        int retryCount = 0;
        String lastErrorInfo = "";

        while (retryCount < MAX_BUILD_RETRIES) {
            log.info("开始构建，应用ID: {}, 第 {} 次尝试", appId, retryCount + 1);

            BuildResult buildResult = executeBuild(projectPath, buildStrategy, appId);

            if (buildResult.isSuccess()) {
                log.info("构建成功，应用ID: {}, 第 {} 次尝试", appId, retryCount + 1);
                return BuildRetryResult.success(retryCount + 1, buildResult);
            }

            log.warn("构建失败，应用ID: {}, 第 {} 次尝试", appId, retryCount + 1);
            lastErrorInfo = buildResult.toErrorSummary();
            log.info("构建错误信息: {}", lastErrorInfo);

            if (retryCount < MAX_BUILD_RETRIES - 1) {
                log.info("尝试使用 AI 修复构建错误，应用ID: {}", appId);
                boolean fixed = fixBuildErrors(projectPath, lastErrorInfo, appId);
                if (!fixed) {
                    log.warn("AI 无法修复构建错误，应用ID: {}", appId);
                    return BuildRetryResult.failed(retryCount + 1, lastErrorInfo);
                }
            }

            retryCount++;
        }

        log.error("构建重试次数已达上限，应用ID: {}", appId);
        return BuildRetryResult.failed(retryCount, lastErrorInfo);
    }

    /**
     * 执行构建
     */
    private BuildResult executeBuild(String projectPath, String buildStrategy, Long appId) {
        switch (buildStrategy) {
            case "vue":
            case "react":
            case "nextjs":
                return vueProjectBuilder.buildProjectWithResult(projectPath);
            case "fullstack":
                return fullstackProjectBuilder.buildProjectWithResult(projectPath, appId);
            default:
                log.warn("未知的构建策略: {}", buildStrategy);
                return BuildResult.failed("detect build strategy", null, "", 0,
                        "未知的构建策略: " + buildStrategy);
        }
    }

    /**
     * 使用 AI 修复构建错误
     */
    private boolean fixBuildErrors(String projectPath, String errorInfo, Long appId) {
        try {
            // 读取所有代码文件
            StringBuilder codeContent = new StringBuilder();
            File projectDir = new File(projectPath);

            FileUtil.walkFiles(projectDir, file -> {
                // 跳过 node_modules 和 dist
                String relativePath = FileUtil.subPath(projectDir.getAbsolutePath(), file.getAbsolutePath());
                if (relativePath.contains("node_modules") || relativePath.contains("dist")) {
                    return;
                }

                // 只读取代码文件
                String name = file.getName().toLowerCase();
                if (name.endsWith(".vue") || name.endsWith(".js") || name.endsWith(".ts") ||
                    name.endsWith(".json") || name.endsWith(".css") || name.endsWith(".html")) {
                    try {
                        String content = FileUtil.readUtf8String(file);
                        codeContent.append("## 文件: ").append(relativePath).append("\n\n");
                        codeContent.append("```").append(getLanguage(name)).append("\n");
                        codeContent.append(content).append("\n");
                        codeContent.append("```\n\n");
                    } catch (Exception e) {
                        log.warn("读取文件失败: {}", file.getName());
                    }
                }
            });

            // 构建修复提示词（转义 {{...}} 防止 LangChain4j 误解析）
            String fixPrompt = String.format(
                INTERNAL_BUILD_FIX_MARKER + "\n\n" +
                "以下 Vue 项目构建失败，请分析错误并修复代码。\n\n" +
                "## 构建错误信息\n```\n%s\n```\n\n" +
                "## 项目代码\n%s\n\n" +
                "请修复代码中的错误，确保项目能够成功构建。\n" +
                "只修改有问题的文件，使用 writeFile 工具写入修复后的完整文件内容。\n" +
                "修复完成后调用 exit 工具结束。",
                errorInfo.replace("{{", "").replace("}}", ""),
                codeContent.toString().replace("{{", "").replace("}}", "")
            );

            // 调用 AI 修复代码
            log.info("调用 AI 修复构建错误，应用ID: {}", appId);
            com.wjh.aicodegen.ai.service.AiCodeGeneratorService fixService =
                    aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(appId, getFixSkill());

            // 使用工具增强模式，让 AI 通过 writeFile 写入修复后的代码
            dev.langchain4j.service.TokenStream tokenStream = fixService.generateCodeWithTools(appId, fixPrompt);

            // 同步等待 AI 修复完成
            java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
            java.util.concurrent.atomic.AtomicBoolean success = new java.util.concurrent.atomic.AtomicBoolean(false);

            tokenStream
                .onToolExecuted(toolExecution -> {
                    log.info("AI 修复工具调用: {}", toolExecution.request().name());
                })
                .onCompleteResponse(response -> {
                    log.info("AI 修复完成，应用ID: {}", appId);
                    success.set(true);
                    latch.countDown();
                })
                .onError(error -> {
                    log.error("AI 修复失败，应用ID: {}", appId, error);
                    latch.countDown();
                })
                .start();

            // 等待 AI 修复完成，最多 2 分钟
            boolean completed = latch.await(2, java.util.concurrent.TimeUnit.MINUTES);
            if (!completed) {
                log.warn("AI 修复超时，应用ID: {}", appId);
                return false;
            }

            return success.get();

        } catch (Exception e) {
            log.error("AI 修复构建错误失败", e);
            return false;
        }
    }

    /**
     * 获取用于修复的 Skill 配置
     */
    private com.wjh.aicodegen.model.entity.CodeSkill getFixSkill() {
        return com.wjh.aicodegen.model.entity.CodeSkill.builder()
                .skillKey("build_fix")
                .name("构建修复")
                .systemPrompt("你是一个代码修复专家。你的任务是修复构建失败的代码。\n\n" +
                        "## 规则\n" +
                        "1. 只修改有问题的文件\n" +
                        "2. 保持代码结构和功能不变\n" +
                        "3. 修复语法错误、缺少的结束标签、错误的导入等\n" +
                        "4. 使用 writeFile 工具写入修复后的完整文件\n" +
                        "5. 修复完成后调用 exit 工具结束")
                .codeGenType("vue_project")
                .buildStrategy("vue")
                .modelStrategy("reasoning")
                .toolNames(null)  // 使用默认工具集
                .build();
    }

    /**
     * 根据文件名获取语言标识
     */
    private String getLanguage(String fileName) {
        if (fileName.endsWith(".vue")) return "vue";
        if (fileName.endsWith(".js")) return "javascript";
        if (fileName.endsWith(".ts")) return "typescript";
        if (fileName.endsWith(".json")) return "json";
        if (fileName.endsWith(".css")) return "css";
        if (fileName.endsWith(".html")) return "html";
        return "";
    }
}
