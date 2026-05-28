package com.wjh.aicodegen.core;

import cn.hutool.json.JSONUtil;
import com.wjh.aicodegen.ai.factory.AiCodeGeneratorServiceFactory;
import com.wjh.aicodegen.ai.model.message.AiResponseMessage;
import com.wjh.aicodegen.ai.model.message.ToolExecutedMessage;
import com.wjh.aicodegen.ai.model.message.ToolRequestMessage;
import com.wjh.aicodegen.ai.service.AiCodeGeneratorService;
import com.wjh.aicodegen.constant.AppConstant;
import com.wjh.aicodegen.core.builder.FullstackProjectBuilder;
import com.wjh.aicodegen.core.builder.VueProjectBuilder;
import com.wjh.aicodegen.core.parser.CodeParserExecutor;
import com.wjh.aicodegen.core.saver.CodeFileSaverExecutor;
import com.wjh.aicodegen.exception.BusinessException;
import com.wjh.aicodegen.exception.ErrorCode;
import com.wjh.aicodegen.manager.TaskCancellationManager;
import com.wjh.aicodegen.model.entity.CodeSkill;
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

import java.io.File;

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

    /**
     * 统一入口：根据 CodeSkill 生成并保存代码（流式）
     *
     * @param userMessage 用户提示词
     * @param skill       代码生成技能
     * @param appId       应用 ID
     */
    public Flux<String> generateAndSaveCodeStream(String userMessage, CodeSkill skill, Long appId) {
        MonitorContext currentContext = ensureMonitorContext(appId);

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
            return processTokenStream(codeStream, appId, currentContext, "auto");
        } else {
            // 工具增强模式（Vue / React / 全栈）：TokenStream → 工具回调 → 构建
            TokenStream codeStream = service.generateCodeWithTools(appId, userMessage);
            return processTokenStream(codeStream, appId, currentContext, buildStrategy);
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
                            Object parsedResult = CodeParserExecutor.executeParser(completeCode, codeGenType);
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
     * 工具增强模式：TokenStream → 工具回调 → 构建
     */
    private Flux<String> processTokenStream(TokenStream tokenStream, Long appId, MonitorContext context,
            String buildStrategy) {
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
                        sink.complete();
                        if (!taskCancellationManager.isTaskCancelled(appId)) {
                            triggerBuild(appId, buildStrategy);
                        } else {
                            log.info("应用 {} 的任务已被取消，跳过项目构建", appId);
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
            case "landing_page", "skill_manager" -> log.info("应用 {} 生成完成，无需构建", appId);
            case "vue", "react", "nextjs" -> {
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
                    vueProjectBuilder.buildProjectAsync(projectDir.getAbsolutePath());
                    log.info("应用 {} 项目构建已启动: {}", appId, projectDir.getName());
                }
            }
            case "fullstack" -> {
                File projectDir = findFullstackProjectDir(appId);
                if (projectDir == null) {
                    log.warn("应用 {} 未找到全栈项目目录，跳过构建", appId);
                    return;
                }
                fullstackProjectBuilder.buildProjectAsync(projectDir.getAbsolutePath(), appId);
                log.info("应用 {} 全栈项目构建已启动: {}", appId, projectDir.getName());
            }
            default -> log.info("应用 {} 构建策略为 {}，跳过构建", appId, buildStrategy);
        }
    }

    /**
     * 自动检测构建策略：扫描生成的文件目录
     */
    private String detectBuildStrategy(Long appId) {
        String outputDir = AppConstant.CODE_OUTPUT_ROOT_DIR;
        // 检查可能的目录名
        String[] possibleDirs = {"fullstack_" + appId, "vue_project_" + appId, "react_ts_" + appId, "nextjs_" + appId};
        for (String dirName : possibleDirs) {
            File dir = new File(outputDir, dirName);
            if (dir.exists() && dir.isDirectory()) {
                if (new File(dir, "schema.sql").exists() || new File(dir, "server").exists()) {
                    return "fullstack";
                }
                if (new File(dir, "package.json").exists()) {
                    // 检查是否是 Next.js
                    if (new File(dir, "next.config.js").exists() || new File(dir, "next.config.mjs").exists()) {
                        return "nextjs";
                    }
                    // 检查是否是 React
                    if (new File(dir, "src/main.tsx").exists() || new File(dir, "vite.config.ts").exists()) {
                        return "react";
                    }
                    // 默认是 Vue
                    return "vue";
                }
            }
        }
        // 检查 html 类型
        File htmlDir = new File(outputDir, "html_" + appId);
        if (htmlDir.exists()) {
            return "none";
        }
        return "none";
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
}
