package com.wjh.aicodegen.core;

import cn.hutool.json.JSONUtil;
import com.wjh.aicodegen.agent.AgentOrchestrator;
import com.wjh.aicodegen.agent.model.ReviewResult;
import com.wjh.aicodegen.ai.factory.AiCodeGeneratorServiceFactory;
import com.wjh.aicodegen.ai.model.message.AiResponseMessage;
import com.wjh.aicodegen.ai.model.message.ReviewResultMessage;
import com.wjh.aicodegen.ai.model.message.ToolExecutedMessage;
import com.wjh.aicodegen.ai.model.message.ToolRequestMessage;
import com.wjh.aicodegen.ai.service.AiCodeGeneratorService;
import com.wjh.aicodegen.constant.AppConstant;
import com.wjh.aicodegen.core.builder.BuildRetryService;
import com.wjh.aicodegen.core.builder.FullstackProjectBuilder;
import com.wjh.aicodegen.core.builder.VueProjectBuilder;
import com.wjh.aicodegen.core.parser.CodeParserExecutor;
import com.wjh.aicodegen.core.saver.CodeFileSaverExecutor;
import com.wjh.aicodegen.exception.BusinessException;
import com.wjh.aicodegen.exception.ErrorCode;
import com.wjh.aicodegen.manager.TaskCancellationManager;
import com.wjh.aicodegen.model.entity.CodeSkill;
import com.wjh.aicodegen.skill.SkillHookExecutor;
import com.wjh.aicodegen.model.enums.AiCallPurposeEnum;
import com.wjh.aicodegen.model.enums.CodeGenTypeEnum;
import com.wjh.aicodegen.monitor.GlobalContextStorage;
import com.wjh.aicodegen.monitor.MonitorContext;
import com.wjh.aicodegen.monitor.MonitorContextHolder;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.ToolExecution;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * AI 代码生成外观类
 * 根据 CodeSkill 的 build_strategy 决定生成和构建流程
 *
 * @author 王哈哈
 */
@Service
@Slf4j
public class AiCodeGeneratorFacade {

    @Resource
    @Lazy
    private AiCodeGeneratorServiceFactory aiCodeGeneratorServiceFactory;
    @Resource
    private VueProjectBuilder vueProjectBuilder;
    @Resource
    private FullstackProjectBuilder fullstackProjectBuilder;
    @Resource
    private TaskCancellationManager taskCancellationManager;
    @Resource
    @Lazy
    private AgentOrchestrator agentOrchestrator;
    @Resource
    private SkillHookExecutor skillHookExecutor;
    @Resource
    private com.wjh.aicodegen.langgraph4j.workflow.ReviewWorkflow reviewWorkflow;
    @Resource
    private BuildRetryService buildRetryService;

    /**
     * 统一入口：根据 CodeSkill 生成并保存代码（流式）
     *
     * @param userMessage 用户提示词
     * @param skill       代码生成技能
     * @param appId       应用 ID
     */
    public Flux<String> generateAndSaveCodeStream(String userMessage, CodeSkill skill, Long appId) {
        MonitorContext currentContext = ensureMonitorContext(appId);

        // 执行 beforeGenerate 钩子（如果有）
        if (skill.getHooks() != null && !skill.getHooks().isBlank()) {
            try {
                java.util.Map<String, Object> hookContext = new java.util.HashMap<>();
                hookContext.put("userMessage", userMessage);
                hookContext.put("appId", String.valueOf(appId));
                hookContext.put("skillKey", skill.getSkillKey());
                skillHookExecutor.executeBeforeGenerate(skill.getHooks(), hookContext);
                log.info("beforeGenerate 钩子执行完成: skill={}", skill.getSkillKey());
            } catch (Exception e) {
                log.warn("beforeGenerate 钩子异常，跳过: {}", e.getMessage());
            }
        }

        // 根据 CodeSkill 获取 AI 服务实例
        AiCodeGeneratorService service = aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(appId, skill);

        String buildStrategy = skill.getBuildStrategy();

        if ("none".equals(buildStrategy)) {
            // 标准模式（HTML / 多文件）：Flux<String> → 解析 → 保存
            CodeGenTypeEnum codeGenType = CodeGenTypeEnum.getEnumByValue(skill.getCodeGenType());
            Flux<String> codeStream = service.generateCode(userMessage);
            return processCodeStream(codeStream, codeGenType, appId, currentContext);
        } else if ("auto".equals(buildStrategy)) {
            // 自动模式：AI 自主选择技能，需要工具支持
            TokenStream codeStream = service.generateCodeWithTools(appId, userMessage);
            return processTokenStream(codeStream, appId, currentContext, "auto", skill);
        } else {
            // 工具增强模式（Vue / React / 全栈）：TokenStream → 工具回调 → 构建
            TokenStream codeStream = service.generateCodeWithTools(appId, userMessage);
            return processTokenStream(codeStream, appId, currentContext, buildStrategy, skill);
        }
    }

    private MonitorContext ensureMonitorContext(Long appId) {
        MonitorContext currentContext = MonitorContextHolder.getContext();
        if (currentContext != null) {
            if (!String.valueOf(appId).equals(currentContext.getAppId())) {
                currentContext.setAppId(String.valueOf(appId));
            }
            if (!AiCallPurposeEnum.CODE_GENERATION.getCode().equals(currentContext.getAiCallPurpose())) {
                currentContext.setAiCallPurpose(AiCallPurposeEnum.CODE_GENERATION.getCode());
            }
            MonitorContextHolder.setContext(currentContext);
        } else {
            currentContext = MonitorContext.builder()
                    .appId(String.valueOf(appId))
                    .userId("system")
                    .aiCallPurpose(AiCallPurposeEnum.CODE_GENERATION.getCode())
                    .build();
            MonitorContextHolder.setContext(currentContext);
        }
        return currentContext;
    }

    /**
     * 标准模式：流式代码 → 解析 → 保存
     */
    private Flux<String> processCodeStream(Flux<String> codeStream, CodeGenTypeEnum codeGenType, Long appId,
            MonitorContext context) {
        StringBuilder codeBuilder = new StringBuilder();
        return codeStream
                .contextWrite(ctx -> {
                    if (context != null) {
                        return MonitorContextHolder.putContextToReactor(ctx, context);
                    }
                    return ctx;
                })
                .doOnNext(codeBuilder::append)
                .doOnComplete(() -> {
                    // 清理全局上下文，防止内存泄漏
                    GlobalContextStorage.removeContext(String.valueOf(appId));
                    try {
                        if (!taskCancellationManager.isTaskCancelled(appId)) {
                            String completeCode = codeBuilder.toString();

                            // 审查工作流：reviewer → (pass | fail → optimizer → reviewer)
                            log.info("启动审查工作流，应用ID: {}", appId);
                            var reviewResult = reviewWorkflow.execute(completeCode, appId);
                            String reviewedCode = reviewResult.getFinalCode();
                            log.info("审查工作流完成: appId={}, status={}, retryCount={}",
                                    appId, reviewResult.getStatus(), reviewResult.getRetryCount());

                            Object parsedResult = CodeParserExecutor.executeParser(reviewedCode, codeGenType);
                            File savedDir = CodeFileSaverExecutor.executeSaver(parsedResult, codeGenType, appId);
                            log.info("保存成功，路径为：" + savedDir.getAbsolutePath());
                        } else {
                            log.info("应用 {} 的任务已被取消，跳过文件保存", appId);
                        }
                    } catch (Exception e) {
                        log.error("保存失败，应用ID: {}", appId, e);
                    }
                })
                .doOnError(e -> {
                    // 清理全局上下文，防止内存泄漏
                    GlobalContextStorage.removeContext(String.valueOf(appId));
                });
    }

    /**
     * 工具增强模式：TokenStream → 工具回调 → Agent 审查 → 构建
     */
    private Flux<String> processTokenStream(TokenStream tokenStream, Long appId, MonitorContext context,
            String buildStrategy, CodeSkill skill) {
        return Flux.<String>create(sink -> {
            tokenStream
                    .onPartialResponse((String partialResponse) -> {
                        AiResponseMessage msg = new AiResponseMessage(partialResponse);
                        sink.next(JSONUtil.toJsonStr(msg));
                    })
                    .onPartialToolCall(partialToolCall -> {
                        ToolRequestMessage msg = new ToolRequestMessage(partialToolCall);
                        sink.next(JSONUtil.toJsonStr(msg));
                    })
                    .onToolExecuted((ToolExecution toolExecution) -> {
                        ToolExecutedMessage msg = new ToolExecutedMessage(toolExecution);
                        sink.next(JSONUtil.toJsonStr(msg));
                    })
                    .onCompleteResponse((ChatResponse response) -> {
                        // 清理全局上下文，防止内存泄漏
                        GlobalContextStorage.removeContext(String.valueOf(appId));

                        // 审查工作流：读取生成的代码文件，运行 reviewer → optimizer 循环
                        if (!taskCancellationManager.isTaskCancelled(appId)) {
                            try {
                                String generatedCode = collectGeneratedCode(appId);
                                if (generatedCode != null && !generatedCode.isBlank()) {
                                    var workflowResult = reviewWorkflow.execute(generatedCode, appId);
                                    ReviewResult reviewResult = workflowResult.getReviewResult();
                                    if (reviewResult != null) {
                                        ReviewResultMessage reviewMsg = new ReviewResultMessage(reviewResult);
                                        sink.next(JSONUtil.toJsonStr(reviewMsg));
                                        log.info("审查工作流完成: appId={}, status={}, score={}, retryCount={}",
                                                appId, workflowResult.getStatus(),
                                                reviewResult.getScore(), workflowResult.getRetryCount());
                                    }
                                }
                            } catch (Exception e) {
                                log.warn("审查工作流异常，跳过: appId={}, error={}", appId, e.getMessage());
                            }

                            // 执行 afterGenerate 钩子（如果有）
                            if (skill.getHooks() != null && !skill.getHooks().isBlank()) {
                                try {
                                    java.util.Map<String, Object> hookContext = new java.util.HashMap<>();
                                    hookContext.put("appId", String.valueOf(appId));
                                    hookContext.put("skillKey", skill.getSkillKey());
                                    skillHookExecutor.executeAfterGenerate(skill.getHooks(), hookContext, "");
                                    log.info("afterGenerate 钩子执行完成: skill={}", skill.getSkillKey());
                                } catch (Exception e) {
                                    log.warn("afterGenerate 钩子异常，跳过: {}", e.getMessage());
                                }
                            }

                            sink.complete();
                            triggerBuild(appId, buildStrategy);
                        } else {
                            log.info("应用 {} 的任务已被取消，跳过项目构建", appId);
                            sink.complete();
                        }
                    })
                    .onError((Throwable e) -> {
                        // 清理全局上下文，防止内存泄漏
                        GlobalContextStorage.removeContext(String.valueOf(appId));
                        log.error("AI 模型调用异常，应用ID: {}", appId, e);
                        sink.error(e);
                    })
                    .start();
        })
                .contextWrite(ctx -> {
                    if (context != null) {
                        return MonitorContextHolder.putContextToReactor(ctx, context);
                    }
                    return ctx;
                });
    }

    /**
     * 根据 build_strategy 触发构建
     */
    private void triggerBuild(Long appId, String buildStrategy) {
        if ("auto".equals(buildStrategy)) {
            // 自动模式：检测生成的文件来决定构建策略
            buildStrategy = detectBuildStrategy(appId);
            log.info("应用 {} 自动检测构建策略: {}", appId, buildStrategy);
        }
        switch (buildStrategy) {
            case "landing_page":
            case "skill_manager":
                log.info("应用 {} 生成完成，无需构建", appId);
                break;
            case "vue":
            case "react":
            case "nextjs":
                // Vue / React / Next.js 都使用 npm install + npm run build
                String projectPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + "fullstack_" + appId;
                File fullstackDir = new File(projectPath);
                File projectDir;
                if (fullstackDir.exists() && new File(fullstackDir, "frontend").exists()) {
                    // 全栈项目，构建前端
                    projectDir = new File(fullstackDir, "frontend");
                } else {
                    // 单项目，直接构建
                    projectDir = findProjectDir(appId);
                }
                if (projectDir != null && projectDir.exists()) {
                    // 使用重试服务进行构建
                    final String finalBuildStrategy = buildStrategy;
                    final File finalProjectDir = projectDir;
                    Thread.ofVirtual().name("build-retry-" + appId).start(() -> {
                        boolean success = buildRetryService.buildWithRetry(
                                finalProjectDir.getAbsolutePath(), finalBuildStrategy, appId);
                        if (success) {
                            log.info("应用 {} 构建成功", appId);
                        } else {
                            log.error("应用 {} 构建失败（已重试）", appId);
                        }
                    });
                }
                break;
            case "fullstack":
                File fullstackProjectDir = findFullstackProjectDir(appId);
                if (fullstackProjectDir == null) {
                    log.warn("应用 {} 未找到全栈项目目录，跳过构建", appId);
                    return;
                }
                // 使用重试服务进行构建
                final File finalFullstackDir = fullstackProjectDir;
                Thread.ofVirtual().name("build-retry-fullstack-" + appId).start(() -> {
                    boolean success = buildRetryService.buildWithRetry(
                            finalFullstackDir.getAbsolutePath(), "fullstack", appId);
                    if (success) {
                        log.info("应用 {} 全栈构建成功", appId);
                    } else {
                        log.error("应用 {} 全栈构建失败（已重试）", appId);
                    }
                });
                break;
            default:
                log.info("应用 {} 构建策略为 {}，跳过构建", appId, buildStrategy);
                break;
        }
    }

    /**
     * 自动检测构建策略：扫描生成的文件目录
     */
    private String detectBuildStrategy(Long appId) {
        String outputDir = AppConstant.CODE_OUTPUT_ROOT_DIR;
        String[] possibleDirs = {"fullstack_" + appId, "vue_project_" + appId, "react_ts_" + appId, "nextjs_" + appId};
        for (String dirName : possibleDirs) {
            File dir = new File(outputDir, dirName);
            if (dir.exists() && dir.isDirectory()) {
                if (new File(dir, "schema.sql").exists() || new File(dir, "server").exists() || new File(dir, "frontend").exists()) {
                    return "fullstack";
                }
                File packageJson = new File(dir, "package.json");
                if (packageJson.exists()) {
                    String packageContent = readFileLowerCase(packageJson);
                    if (new File(dir, "next.config.js").exists()
                            || new File(dir, "next.config.mjs").exists()
                            || new File(dir, "next.config.ts").exists()
                            || packageContent.contains("\"next\"")
                            || packageContent.contains("'next'")) {
                        return "nextjs";
                    }
                    if (packageContent.contains("@vitejs/plugin-react")
                            || packageContent.contains("react")
                            || new File(dir, "src/main.tsx").exists()
                            || new File(dir, "src/main.jsx").exists()) {
                        return "react";
                    }
                    if (packageContent.contains("@vitejs/plugin-vue")
                            || packageContent.contains("vue")
                            || new File(dir, "src/main.ts").exists()
                            || new File(dir, "src/App.vue").exists()) {
                        return "vue";
                    }
                    return "vue";
                }
            }
        }
        File htmlDir = new File(outputDir, "html_" + appId);
        if (htmlDir.exists()) {
            return "none";
        }
        return "none";
    }

    private String readFileLowerCase(File file) {
        try {
            return Files.readString(file.toPath()).toLowerCase();
        } catch (Exception e) {
            log.warn("读取文件失败: {}", file.getAbsolutePath(), e);
            return "";
        }
    }

    /**
     * 查找全栈项目目录
     */
    private File findFullstackProjectDir(Long appId) {
        String outputDir = AppConstant.CODE_OUTPUT_ROOT_DIR;
        String[] possibleDirs = {"fullstack_" + appId, "vue_project_" + appId, "react_ts_" + appId, "nextjs_" + appId};
        for (String dirName : possibleDirs) {
            File dir = new File(outputDir, dirName);
            if (dir.exists() && new File(dir, "frontend").exists() && new File(dir, "server").exists()) {
                return dir;
            }
        }
        return null;
    }

    /**
     * 查找项目目录
     */
    private File findProjectDir(Long appId) {
        String outputDir = AppConstant.CODE_OUTPUT_ROOT_DIR;
        String[] possibleDirs = {"fullstack_" + appId, "vue_project_" + appId, "react_ts_" + appId, "nextjs_" + appId};
        for (String dirName : possibleDirs) {
            File dir = new File(outputDir, dirName);
            if (dir.exists()) return dir;
        }
        return null;
    }

    /**
     * 收集生成的代码文件内容（供审查工作流使用）
     */
    private String collectGeneratedCode(Long appId) {
        File projectDir = findProjectDir(appId);
        if (projectDir == null || !projectDir.exists()) {
            log.warn("未找到生成目录，跳过审查: appId={}", appId);
            return null;
        }

        List<String> codeFiles = collectCodeFiles(projectDir.toPath());
        if (codeFiles.isEmpty()) {
            log.warn("未找到代码文件，跳过审查: appId={}", appId);
            return null;
        }

        String combinedCode = String.join("\n\n", codeFiles);
        if (combinedCode.length() > 50000) {
            log.info("代码过长（{} 字符），截取前 50000 字符进行审查", combinedCode.length());
            combinedCode = combinedCode.substring(0, 50000);
        }
        return combinedCode;
    }

    /**
     * 递归收集目录下的代码文件内容
     */
    private List<String> collectCodeFiles(Path dir) {
        List<String> contents = new ArrayList<>();
        String[] extensions = {".html", ".css", ".js", ".vue", ".ts", ".tsx", ".jsx", ".json", ".sql", ".md"};
        try (Stream<Path> walk = Files.walk(dir, 10)) {
            walk.filter(Files::isRegularFile)
                    .filter(p -> {
                        String name = p.getFileName().toString().toLowerCase();
                        for (String ext : extensions) {
                            if (name.endsWith(ext)) return true;
                        }
                        return false;
                    })
                    .filter(p -> {
                        // 排除 node_modules, dist, .git 等
                        String pathStr = p.toString().toLowerCase();
                        return !pathStr.contains("node_modules") &&
                               !pathStr.contains(File.separator + "dist" + File.separator) &&
                               !pathStr.contains(".git");
                    })
                    .forEach(p -> {
                        try {
                            String content = Files.readString(p);
                            String relativePath = dir.relativize(p).toString();
                            contents.add("// File: " + relativePath + "\n" + content);
                        } catch (IOException e) {
                            log.warn("读取文件失败: {}", p);
                        }
                    });
        } catch (IOException e) {
            log.error("遍历目录失败: {}", dir, e);
        }
        return contents;
    }
}
