package com.wjh.aicodegen.core;

import cn.hutool.json.JSONUtil;
import com.mybatisflex.core.update.UpdateChain;
import com.wjh.aicodegen.agent.AgentOrchestrator;
import com.wjh.aicodegen.ai.factory.AiCodeGeneratorServiceFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.wjh.aicodegen.ai.model.message.AiResponseMessage;
import com.wjh.aicodegen.ai.model.message.ThinkingMessage;
import com.wjh.aicodegen.ai.model.message.ToolExecutedMessage;
import com.wjh.aicodegen.ai.model.message.ToolRequestMessage;
import com.wjh.aicodegen.ai.service.AiCodeGeneratorService;
import com.wjh.aicodegen.constant.AppConstant;
import com.wjh.aicodegen.core.builder.BuildRetryResult;
import com.wjh.aicodegen.core.builder.BuildRetryService;
import com.wjh.aicodegen.core.builder.FullstackProjectBuilder;
import com.wjh.aicodegen.core.builder.VueProjectBuilder;
import com.wjh.aicodegen.core.parser.CodeParserExecutor;
import com.wjh.aicodegen.core.saver.CodeFileSaverExecutor;
import com.wjh.aicodegen.core.saver.GeneratedProjectWorkspace;
import com.wjh.aicodegen.core.validator.GeneratedProjectValidator;
import com.wjh.aicodegen.exception.BusinessException;
import com.wjh.aicodegen.exception.ErrorCode;
import com.wjh.aicodegen.manager.BuildEventSinkManager;
import com.wjh.aicodegen.manager.GenerationStreamManager;
import com.wjh.aicodegen.manager.GenerationTaskManager;
import com.wjh.aicodegen.manager.GenerationTaskState;
import com.wjh.aicodegen.manager.TaskCancellationManager;
import com.wjh.aicodegen.model.entity.App;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    @Resource
    private BuildEventSinkManager buildEventSinkManager;
    @Resource
    private GeneratedProjectValidator generatedProjectValidator;
    @Resource
    private GenerationTaskManager generationTaskManager;
    @Resource
    private GenerationStreamManager generationStreamManager;

    /**
     * 统一入口：根据 CodeSkill 生成并保存代码（流式）
     *
     * @param userMessage 用户提示词
     * @param skill       代码生成技能
     * @param appId       应用 ID
     */
    private static final int MAX_RETRY_COUNT = 3;
    private static final int STANDARD_STREAM_IDLE_TIMEOUT_SECONDS = 240;
    private static final int TOOL_STREAM_TOTAL_TIMEOUT_SECONDS = 600;

    public Flux<String> generateAndSaveCodeStream(String userMessage, CodeSkill skill, Long appId) {
        return Flux.defer(() -> {
            MonitorContext currentContext = ensureMonitorContext(appId);
            long startTime = System.currentTimeMillis();
            log.info("开始生成代码: appId={}, skill={}, buildStrategy={}, modelStrategy={}, prompt长度={}",
                    appId, skill.getSkillKey(), skill.getBuildStrategy(), skill.getModelStrategy(),
                    userMessage != null ? userMessage.length() : 0);

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
                return processCodeStream(codeStream, codeGenType, appId, currentContext, startTime);
            } else if ("auto".equals(buildStrategy)) {
                // 自动模式：AI 自主选择技能，需要工具支持
                TokenStream codeStream = service.generateCodeWithTools(appId, userMessage);
                return processTokenStream(codeStream, appId, currentContext, "auto", skill, startTime);
            } else {
                // 工具增强模式（Vue / React / 全栈）：TokenStream → 工具回调 → 构建
                TokenStream codeStream = service.generateCodeWithTools(appId, userMessage);
                return processTokenStream(codeStream, appId, currentContext, buildStrategy, skill, startTime);
            }
        }).retryWhen(reactor.util.retry.Retry.backoff(MAX_RETRY_COUNT, java.time.Duration.ofSeconds(2))
                .filter(this::isJsonParseError)
                .doBeforeRetry(retrySignal -> {
                    log.info("重试生成代码: appId={}, 第{}次重试", appId, retrySignal.totalRetries() + 1);
                }));
    }

    private boolean isJsonParseError(Throwable e) {
        if (e instanceof com.fasterxml.jackson.core.JsonParseException) {
            return true;
        }
        if (e.getCause() != null) {
            return isJsonParseError(e.getCause());
        }
        if (e.getMessage() != null && e.getMessage().contains("JsonParseException")) {
            return true;
        }
        return false;
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
            MonitorContext context, long startTime) {
        StringBuilder codeBuilder = new StringBuilder();
        long[] firstTokenTime = {0};
        return codeStream
                .timeout(java.time.Duration.ofSeconds(STANDARD_STREAM_IDLE_TIMEOUT_SECONDS),
                        Flux.error(new BusinessException(ErrorCode.OPERATION_ERROR,
                                "AI 生成响应超时，超过 " + STANDARD_STREAM_IDLE_TIMEOUT_SECONDS + " 秒未收到新内容，请稍后重试")))
                .contextWrite(ctx -> {
                    if (context != null) {
                        return MonitorContextHolder.putContextToReactor(ctx, context);
                    }
                    return ctx;
                })
                .doOnNext(token -> {
                    if (firstTokenTime[0] == 0) {
                        firstTokenTime[0] = System.currentTimeMillis();
                        log.info("收到首个token: appId={}, 耗时={}ms", appId, firstTokenTime[0] - startTime);
                    }
                    codeBuilder.append(token);
                })
                .doOnComplete(() -> {
                    long totalTime = System.currentTimeMillis() - startTime;
                    long tokenTime = firstTokenTime[0] > 0 ? System.currentTimeMillis() - firstTokenTime[0] : 0;
                    long firstTokenCost = firstTokenTime[0] > 0 ? firstTokenTime[0] - startTime : -1;
                    log.info("代码生成完成: appId={}, 总耗时={}ms, 首token耗时={}ms, 生成耗时={}ms, 代码长度={}",
                            appId, totalTime, firstTokenCost, tokenTime, codeBuilder.length());

                    // 清理全局上下文，防止内存泄漏
                    GlobalContextStorage.removeContext(String.valueOf(appId));
                    try {
                        if (!taskCancellationManager.isTaskCancelled(appId)) {
                            String completeCode = codeBuilder.toString();
                            emitGenerationState(generationTaskManager.markValidating(appId, "正在校验生成代码完整性"));
                            Object parsedResult = CodeParserExecutor.executeParser(completeCode, codeGenType);
                            File savedDir = CodeFileSaverExecutor.executeSaver(parsedResult, codeGenType, appId);
                            generatedProjectValidator.validateStaticProject(savedDir, codeGenType);
                            emitGenerationState(generationTaskManager.succeed(appId, "代码生成完成", savedDir.getAbsolutePath()));
                            log.info("保存成功，路径为：" + savedDir.getAbsolutePath());
                            runReviewWorkflowAsync(completeCode, appId);
                        } else {
                            log.info("应用 {} 的任务已被取消，跳过文件保存", appId);
                        }
                    } catch (Exception e) {
                        log.error("保存失败，应用ID: {}", appId, e);
                        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "代码保存失败: " + e.getMessage());
                    }
                })
                .onErrorResume(e -> {
                    long totalTime = System.currentTimeMillis() - startTime;
                    // 如果已经有生成内容，优先尝试保存；保存失败必须继续按失败处理，不能伪装成成功。
                    if (codeBuilder.length() > 100) {
                        log.warn("代码生成遇到错误但已有内容，尝试保存: appId={}, 耗时={}ms, 错误={}, 代码长度={}",
                                appId, totalTime, e.getMessage(), codeBuilder.length());
                        GlobalContextStorage.removeContext(String.valueOf(appId));
                        try {
                            if (!taskCancellationManager.isTaskCancelled(appId)) {
                                String completeCode = codeBuilder.toString();
                                emitGenerationState(generationTaskManager.markValidating(appId, "正在尝试保存已生成内容"));
                                Object parsedResult = CodeParserExecutor.executeParser(completeCode, codeGenType);
                                File savedDir = CodeFileSaverExecutor.executeSaver(parsedResult, codeGenType, appId);
                                generatedProjectValidator.validateStaticProject(savedDir, codeGenType);
                                emitGenerationState(generationTaskManager.succeed(appId, "代码生成完成", savedDir.getAbsolutePath()));
                                log.info("错误恢复保存成功，路径为：" + savedDir.getAbsolutePath());
                                runReviewWorkflowAsync(completeCode, appId);
                            }
                            return Flux.empty();
                        } catch (Exception saveError) {
                            log.error("错误恢复保存失败，应用ID: {}", appId, saveError);
                            return Flux.error(saveError);
                        }
                    }
                    // 没有足够内容，返回错误
                    log.error("代码生成异常: appId={}, 耗时={}ms, 错误={}", appId, totalTime, e.getMessage());
                    GlobalContextStorage.removeContext(String.valueOf(appId));
                    return Flux.error(e);
                });
    }

    /**
     * 工具增强模式：TokenStream → 工具回调 → Agent 审查 → 构建
     */
    private Flux<String> processTokenStream(TokenStream tokenStream, Long appId, MonitorContext context,
            String buildStrategy, CodeSkill skill, long startTime) {
        long[] firstTokenTime = {0};
        return Flux.<String>create(sink -> {
            tokenStream
                    .onPartialResponse((String partialResponse) -> {
                        if (firstTokenTime[0] == 0) {
                            firstTokenTime[0] = System.currentTimeMillis();
                            log.info("收到首个token: appId={}, 耗时={}ms", appId, firstTokenTime[0] - startTime);
                        }
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
                        long totalTime = System.currentTimeMillis() - startTime;
                        long tokenTime = firstTokenTime[0] > 0 ? System.currentTimeMillis() - firstTokenTime[0] : 0;
                        log.info("代码生成完成: appId={}, 总耗时={}ms, 首token耗时={}ms, 生成耗时={}ms",
                                appId, totalTime, firstTokenTime[0] - startTime, tokenTime);

                        // 发送 thinking 内容（通过反射检测 reasoningContent 方法，兼容不同版本 LangChain4j）
                        try {
                            var aiMessage = response.aiMessage();
                            if (aiMessage != null) {
                                var method = aiMessage.getClass().getMethod("reasoningContent");
                                Object reasoningObj = method.invoke(aiMessage);
                                String reasoningContent = reasoningObj instanceof String ? (String) reasoningObj : null;
                                if (reasoningContent != null && !reasoningContent.isBlank()) {
                                    log.info("捕获到thinking内容: appId={}, 长度={}", appId, reasoningContent.length());
                                    ThinkingMessage thinkingMsg = new ThinkingMessage(reasoningContent);
                                    sink.next(JSONUtil.toJsonStr(thinkingMsg));
                                }
                            }
                        } catch (NoSuchMethodException e) {
                            log.debug("当前LangChain4j版本不支持reasoningContent");
                        } catch (Exception e) {
                            log.debug("获取reasoningContent失败: {}", e.getMessage());
                        }

                        // 清理全局上下文，防止内存泄漏
                        GlobalContextStorage.removeContext(String.valueOf(appId));

                        // 后台审查不影响用户侧生成完成状态
                        if (!taskCancellationManager.isTaskCancelled(appId)) {
                            try {
                                emitGenerationState(generationTaskManager.markValidating(appId, "正在校验生成项目结构"));
                                validateGeneratedProject(appId, buildStrategy, skill);
                                commitGeneratedWorkspace(appId);
                            } catch (Exception e) {
                                log.error("生成产物结构校验失败: appId={}, buildStrategy={}, skill={}, error={}",
                                        appId, buildStrategy, skill.getSkillKey(), e.getMessage(), e);
                                updateBuildStatus(appId, "failed", "生成产物不完整", e.getMessage(), 0);
                                emitGenerationState(generationTaskManager.fail(appId, "生成产物不完整", e.getMessage()));
                                sink.error(e);
                                return;
                            }

                            runGeneratedProjectReviewAsync(appId);

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
                        long totalTime = System.currentTimeMillis() - startTime;
                        log.error("代码生成异常: appId={}, 耗时={}ms, 错误={}", appId, totalTime, e.getMessage());
                        // 清理全局上下文，防止内存泄漏
                        GlobalContextStorage.removeContext(String.valueOf(appId));
                        sink.error(e);
                    })
                    .start();
        })
                .timeout(java.time.Duration.ofSeconds(resolveToolStreamTimeoutSeconds(buildStrategy)),
                        Flux.error(new BusinessException(ErrorCode.OPERATION_ERROR,
                                "AI 工程生成超时，请简化需求或稍后重试")))
                .contextWrite(ctx -> {
                    if (context != null) {
                        return MonitorContextHolder.putContextToReactor(ctx, context);
                    }
                    return ctx;
                });
    }

    private int resolveToolStreamTimeoutSeconds(String buildStrategy) {
        if ("fullstack".equals(buildStrategy)) {
            return TOOL_STREAM_TOTAL_TIMEOUT_SECONDS;
        }
        if ("vue".equals(buildStrategy) || "react".equals(buildStrategy) || "nextjs".equals(buildStrategy)) {
            return 420;
        }
        return 300;
    }

    /**
     * 手动触发已有项目重新构建
     */
    public void rebuildGeneratedProject(Long appId, String buildStrategy) {
        triggerBuild(appId, buildStrategy);
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
            case "none":
                log.info("应用 {} 生成完成，无需构建", appId);
                updateBuildStatus(appId, "none", "无需构建", null, 0);
                File noBuildProjectDir = findProjectDir(appId);
                emitGenerationState(generationTaskManager.succeed(appId, "代码生成完成", noBuildProjectDir == null ? null : noBuildProjectDir.getAbsolutePath()));
                generationStreamManager.complete(appId);
                break;
            case "vue":
            case "react":
            case "nextjs":
                // Vue / React / Next.js 都使用 npm install + npm run build
                String projectPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + "fullstack_" + appId;
                File fullstackDir = new File(projectPath);
                File projectDir;
                if (fullstackDir.exists() && new File(fullstackDir, "frontend").exists()) {
                    projectDir = new File(fullstackDir, "frontend");
                } else {
                    projectDir = findProjectDir(appId);
                }
                if (projectDir != null && projectDir.exists()) {
                    runBuildAsync(appId, buildStrategy, projectDir, "应用");
                } else {
                    log.warn("应用 {} 未找到项目目录，跳过构建", appId);
                    updateBuildStatus(appId, "failed", "未找到项目目录", "未找到可构建的项目目录", 0);
                    emitGenerationState(generationTaskManager.fail(appId, "未找到项目目录", "未找到可构建的项目目录"));
                    generationStreamManager.complete(appId);
                }
                break;
            case "fullstack":
                File fullstackProjectDir = findFullstackProjectDir(appId);
                if (fullstackProjectDir == null) {
                    log.warn("应用 {} 未找到全栈项目目录，跳过构建", appId);
                    updateBuildStatus(appId, "failed", "未找到全栈项目目录", "未找到 server/ 和 frontend/ 目录", 0);
                    emitGenerationState(generationTaskManager.fail(appId, "未找到全栈项目目录", "未找到 server/ 和 frontend/ 目录"));
                    generationStreamManager.complete(appId);
                    return;
                }
                runBuildAsync(appId, "fullstack", fullstackProjectDir, "全栈应用");
                break;
            default:
                log.info("应用 {} 构建策略为 {}，跳过构建", appId, buildStrategy);
                updateBuildStatus(appId, "none", "无需构建", null, 0);
                File defaultProjectDir = findProjectDir(appId);
                emitGenerationState(generationTaskManager.succeed(appId, "代码生成完成", defaultProjectDir == null ? null : defaultProjectDir.getAbsolutePath()));
                generationStreamManager.complete(appId);
                break;
        }
    }

    private void runBuildAsync(Long appId, String buildStrategy, File projectDir, String label) {
        updateBuildStatus(appId, "building", "构建中", null, 0);
        emitGenerationState(generationTaskManager.markBuilding(appId, label + "正在构建"));
        Thread.ofVirtual().name("build-retry-" + appId).start(() -> {
            BuildRetryResult result = buildRetryService.buildWithRetry(
                    projectDir.getAbsolutePath(), buildStrategy, appId);
            if (result.isSuccess()) {
                log.info("{} {} 构建成功", label, appId);
                updateBuildStatus(appId, "success", "构建成功", null, result.getRetryCount());
                emitGenerationState(generationTaskManager.succeed(appId, "构建成功，应用已完整生成", projectDir.getAbsolutePath()));
                generationStreamManager.complete(appId);
            } else {
                log.error("{} {} 构建失败（已重试）", label, appId);
                updateBuildStatus(appId, "failed", "构建失败", result.getErrorSummary(), result.getRetryCount());
                emitGenerationState(generationTaskManager.fail(appId, "构建失败", result.getErrorSummary()));
                generationStreamManager.complete(appId);
            }
        });
    }

    private void updateBuildStatus(Long appId, String status, String message, String error, Integer retryCount) {
        LocalDateTime now = LocalDateTime.now();
        String safeError = normalizeBuildError(error);
        UpdateChain<App> update = UpdateChain.of(App.class)
                .set(App::getBuildStatus, status)
                .set(App::getBuildMessage, message)
                .set(App::getBuildError, safeError)
                .set(App::getBuildRetryCount, retryCount == null ? 0 : retryCount);

        if ("building".equals(status)) {
            update.set(App::getBuildStartedTime, now)
                    .set(App::getBuildFinishedTime, null);
        } else {
            update.set(App::getBuildFinishedTime, now);
        }

        boolean updated = update.where(App::getId).eq(appId).update();
        if (!updated) {
            log.warn("更新应用构建状态失败: appId={}, status={}", appId, status);
        }
        emitBuildStatus(appId, status, message, safeError, retryCount == null ? 0 : retryCount);
    }

    private void emitBuildStatus(Long appId, String status, String message, String error, int retryCount) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("type", "build_status");
        event.put("appId", appId);
        event.put("status", status);
        event.put("message", message);
        event.put("retryCount", retryCount);
        event.put("error", error);
        event.put("timestamp", LocalDateTime.now().toString());
        buildEventSinkManager.emit(appId, JSONUtil.toJsonStr(event));
    }

    private String normalizeBuildError(String error) {
        if (error == null || error.isBlank()) {
            return null;
        }
        String trimmed = error.trim();
        int maxLength = 4000;
        if (trimmed.length() <= maxLength) {
            return trimmed;
        }
        return trimmed.substring(0, maxLength) + "\n...（错误信息已截断）";
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

    private File commitGeneratedWorkspace(Long appId) {
        String[] possibleDirs = {"fullstack_" + appId, "vue_project_" + appId, "react_ts_" + appId, "nextjs_" + appId,
                "landing_page_" + appId, "html_" + appId, "multi_file_" + appId};
        for (String dirName : possibleDirs) {
            File stagingDir = GeneratedProjectWorkspace.stagingDir(dirName);
            if (stagingDir.exists() && stagingDir.isDirectory()) {
                return GeneratedProjectWorkspace.commit(dirName);
            }
        }
        File projectDir = findProjectDir(appId);
        if (projectDir != null) {
            return projectDir;
        }
        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "未找到可提交的生成目录");
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

    private void emitGenerationState(GenerationTaskState state) {
        if (state != null && state.getAppId() != null) {
            generationStreamManager.emit(state.getAppId(), generationTaskManager.toStateEvent(state));
        }
    }

    private void runReviewWorkflowAsync(String generatedCode, Long appId) {
        if (generatedCode == null || generatedCode.isBlank()) {
            return;
        }
        Thread.ofVirtual().name("review-workflow-" + appId).start(() -> {
            try {
                log.info("启动后台审查工作流，应用ID: {}", appId);
                var reviewResult = reviewWorkflow.execute(generatedCode, appId);
                log.info("后台审查工作流完成: appId={}, status={}, retryCount={}",
                        appId, reviewResult.getStatus(), reviewResult.getRetryCount());
            } catch (Exception e) {
                log.warn("后台审查工作流异常，跳过: appId={}, error={}", appId, e.getMessage());
            }
        });
    }

    private void runGeneratedProjectReviewAsync(Long appId) {
        Thread.ofVirtual().name("review-generated-project-" + appId).start(() -> {
            try {
                String generatedCode = collectGeneratedCode(appId);
                if (generatedCode == null || generatedCode.isBlank()) {
                    return;
                }
                log.info("启动后台项目审查工作流，应用ID: {}", appId);
                var workflowResult = reviewWorkflow.execute(generatedCode, appId);
                log.info("后台项目审查工作流完成: appId={}, status={}, retryCount={}",
                        appId, workflowResult.getStatus(), workflowResult.getRetryCount());
            } catch (Exception e) {
                log.warn("后台项目审查工作流异常，跳过: appId={}, error={}", appId, e.getMessage());
            }
        });
    }

    private void validateGeneratedProject(Long appId, String buildStrategy, CodeSkill skill) {
        if (buildStrategy == null || buildStrategy.isBlank()) {
            return;
        }
        File projectDir = findReadableProjectDir(appId);
        if (projectDir == null || !projectDir.exists()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "未找到生成目录");
        }

        generatedProjectValidator.validateBuildProject(projectDir, buildStrategy);
    }

    private File findReadableProjectDir(Long appId) {
        String[] possibleDirs = {"fullstack_" + appId, "vue_project_" + appId, "react_ts_" + appId, "nextjs_" + appId,
                "landing_page_" + appId, "html_" + appId, "multi_file_" + appId};
        for (String dirName : possibleDirs) {
            File stagingDir = GeneratedProjectWorkspace.stagingDir(dirName);
            if (stagingDir.exists() && stagingDir.isDirectory()) {
                return stagingDir;
            }
        }
        return findProjectDir(appId);
    }

    private void validateVueProject(File projectDir) {
        requireFile(projectDir, "package.json");
        requireFile(projectDir, "index.html");
        requireAnyFile(projectDir, "src/main.ts", "src/main.js");
        requireAnyFile(projectDir, "src/App.vue", "src/app.vue");
        String packageJson = readFileLowerCase(new File(projectDir, "package.json"));
        if (!packageJson.contains("vue") || !packageJson.contains("vite")) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Vue 项目 package.json 缺少 vue/vite 依赖");
        }
    }

    private void validateReactProject(File projectDir) {
        requireFile(projectDir, "package.json");
        requireFile(projectDir, "index.html");
        requireAnyFile(projectDir, "src/main.tsx", "src/main.jsx");
        requireAnyFile(projectDir, "src/App.tsx", "src/App.jsx");
        String packageJson = readFileLowerCase(new File(projectDir, "package.json"));
        if (!packageJson.contains("react") || !packageJson.contains("vite")) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "React 项目 package.json 缺少 react/vite 依赖");
        }
    }

    private void validateNextProject(File projectDir) {
        requireFile(projectDir, "package.json");
        requireAnyFile(projectDir, "app/page.tsx", "src/app/page.tsx", "pages/index.tsx", "src/pages/index.tsx");
        String packageJson = readFileLowerCase(new File(projectDir, "package.json"));
        if (!packageJson.contains("next")) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Next.js 项目 package.json 缺少 next 依赖");
        }
    }

    private void validateFullstackProject(File projectDir) {
        File frontendDir = new File(projectDir, "frontend");
        File serverDir = new File(projectDir, "server");
        if (!frontendDir.exists() || !serverDir.exists()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "全栈项目必须同时包含 frontend 和 server 目录");
        }
        validateVueProject(frontendDir);
        requireFile(serverDir, "package.json");
        requireAnyFile(serverDir, "index.js", "index.ts", "src/index.js", "src/index.ts");
        String packageJson = readFileLowerCase(new File(serverDir, "package.json"));
        if (!packageJson.contains("express") || !packageJson.contains("mysql2")) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "全栈后端 package.json 缺少 express/mysql2 依赖");
        }
    }

    private void requireFile(File baseDir, String relativePath) {
        File file = new File(baseDir, relativePath);
        if (!file.exists() || !file.isFile()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "缺少必要文件: " + relativePath);
        }
    }

    private void requireAnyFile(File baseDir, String... relativePaths) {
        for (String relativePath : relativePaths) {
            File file = new File(baseDir, relativePath);
            if (file.exists() && file.isFile()) {
                return;
            }
        }
        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "缺少必要文件: " + String.join(", ", relativePaths));
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
