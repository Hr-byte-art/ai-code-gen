package com.wjh.aicodegen.core;

import cn.hutool.json.JSONUtil;
import com.wjh.aicodegen.ai.factory.AiCodeGeneratorServiceFactory;
import com.wjh.aicodegen.ai.model.message.AiResponseMessage;
import com.wjh.aicodegen.ai.model.message.ToolExecutedMessage;
import com.wjh.aicodegen.ai.model.message.ToolRequestMessage;
import com.wjh.aicodegen.ai.service.AiCodeGeneratorService;
import com.wjh.aicodegen.constant.AppConstant;
import com.wjh.aicodegen.core.builder.VueProjectBuilder;
import com.wjh.aicodegen.core.parser.CodeParserExecutor;
import com.wjh.aicodegen.core.saver.CodeFileSaverExecutor;
import com.wjh.aicodegen.exception.BusinessException;
import com.wjh.aicodegen.exception.ErrorCode;
import com.wjh.aicodegen.manager.TaskCancellationManager;
import com.wjh.aicodegen.model.enums.AiCallPurposeEnum;
import com.wjh.aicodegen.model.enums.CodeGenTypeEnum;
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
 * AI 代码生成外观类，组合生成和保存功能
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
    private TaskCancellationManager taskCancellationManager;

    /**
     * 统一入口：根据类型生成并保存代码（流式，使用 appId）
     *
     * @param userMessage     用户提示词
     * @param codeGenTypeEnum 生成类型
     * @param appId           应用 ID
     */
    public Flux<String> generateAndSaveCodeStream(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId) {
        // 获取当前的MonitorContext，确保在AI调用前传递
        MonitorContext currentContext = MonitorContextHolder.getContext();
        if (currentContext != null) {
            log.info("在AI代码生成前获取到MonitorContext: userId={}, appId={}, aiCallPurpose={}",
                    currentContext.getUserId(), currentContext.getAppId(), currentContext.getAiCallPurpose());

            // 确保appId和codeGenType一致
            if (!String.valueOf(appId).equals(currentContext.getAppId())) {
                log.warn("MonitorContext中的appId与参数不匹配: context={}, param={}", currentContext.getAppId(), appId);
                currentContext.setAppId(String.valueOf(appId));
            }
            // 更新AI调用用途为代码生成
            if (!AiCallPurposeEnum.CODE_GENERATION.getCode().equals(currentContext.getAiCallPurpose())) {
                log.debug("更新MonitorContext的aiCallPurpose: {} -> CODE_GENERATION", currentContext.getAiCallPurpose());
                currentContext.setAiCallPurpose(AiCallPurposeEnum.CODE_GENERATION.getCode());
            }
            MonitorContextHolder.setContext(currentContext);
        } else {
            log.warn("在AI代码生成前未获取到MonitorContext，尝试创建默认上下文: appId={}", appId);
            // 创建一个默认的上下文
            currentContext = MonitorContext.builder()
                    .appId(String.valueOf(appId))
                    .userId("system") // 使用system避免unknown
                    .aiCallPurpose(AiCallPurposeEnum.CODE_GENERATION.getCode()) // 设置AI调用用途
                    .build();
            MonitorContextHolder.setContext(currentContext);
        }

        // 根据 appId 获取对应的 AI 服务实例
        AiCodeGeneratorService aiCodeGeneratorService = aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(appId,
                codeGenTypeEnum);
        return switch (codeGenTypeEnum) {
            case HTML -> {
                Flux<String> codeStream = aiCodeGeneratorService.generateHtmlCodeStream(userMessage);
                yield processCodeStream(codeStream, CodeGenTypeEnum.HTML, appId, currentContext);
            }
            case MULTI_FILE -> {
                Flux<String> codeStream = aiCodeGeneratorService.generateMultiFileCodeStream(userMessage);
                yield processCodeStream(codeStream, CodeGenTypeEnum.MULTI_FILE, appId, currentContext);
            }
            case VUE_PROJECT -> {
                TokenStream codeStream = aiCodeGeneratorService.generateVueProjectCodeStream(appId, userMessage);
                yield processTokenStream(codeStream, appId, currentContext);
            }
            default -> {
                String errorMessage = "不支持的生成类型：" + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };
    }

    /**
     * 通用流式代码处理方法（使用 appId）
     *
     * @param codeStream  代码流
     * @param codeGenType 代码生成类型
     * @param appId       应用 ID
     * @param context     监控上下文
     * @return 流式响应
     */
    private Flux<String> processCodeStream(Flux<String> codeStream, CodeGenTypeEnum codeGenType, Long appId,
            MonitorContext context) {
        StringBuilder codeBuilder = new StringBuilder();
        // 实时收集代码片段，并传递MonitorContext到Reactor Context
        return codeStream
                .contextWrite(ctx -> {
                    if (context != null) {
                        return MonitorContextHolder.putContextToReactor(ctx, context);
                    }
                    return ctx;
                })
                .doOnNext(codeBuilder::append)
                .doOnComplete(() -> {
                    // 流式返回完成后保存代码
                    log.info("应用 {} 的AI代码生成流已完成，准备保存文件", appId);
                    try {
                        // 检查任务是否被取消
                        if (!taskCancellationManager.isTaskCancelled(appId)) {
                            log.info("应用 {} 任务未被取消，开始保存文件", appId);
                            String completeCode = codeBuilder.toString();
                            // 使用执行器解析代码
                            Object parsedResult = CodeParserExecutor.executeParser(completeCode, codeGenType);
                            // 使用执行器保存代码
                            File savedDir = CodeFileSaverExecutor.executeSaver(parsedResult, codeGenType, appId);
                            log.info("保存成功，路径为：" + savedDir.getAbsolutePath());
                        } else {
                            log.info("应用 {} 的任务已被取消，跳过文件保存", appId);
                        }
                    } catch (Exception e) {
                        log.error("保存失败，应用ID: {}", appId, e);
                    }
                });
    }

    /**
     * 将 TokenStream 转换为 Flux<String>，并传递工具调用信息
     *
     * @param tokenStream TokenStream 对象
     * @param appId       应用 ID
     * @param context     监控上下文
     * @return Flux<String> 流式响应
     */
    private Flux<String> processTokenStream(TokenStream tokenStream, Long appId, MonitorContext context) {
        return Flux.<String>create(sink -> {
            tokenStream
                    /*
                     * 作用：处理 AI 模型返回的部分响应内容
                     * 触发时机：当 AI 模型逐步生成响应文本时，会分批返回文本片段
                     * 用途：用于实现实时流式输出，让用户能够逐步看到 AI 生成的内容，而不需要等待完整响应
                     */
                    .onPartialResponse((String partialResponse) -> {
                        AiResponseMessage aiResponseMessage = new AiResponseMessage(partialResponse);
                        sink.next(JSONUtil.toJsonStr(aiResponseMessage));
                    })
                    /*
                     * 作用：处理 AI 模型返回的工具调用信息
                     * 触发时机：当 AI 模型需要调用工具时，会返回工具调用信息添加 事件处理的作用 的注释
                     * 用途：用于实现工具调用的实时反馈，让用户能够看到工具调用的进度和结果
                     */
                    .onPartialToolExecutionRequest((index, toolExecutionRequest) -> {
                        ToolRequestMessage toolRequestMessage = new ToolRequestMessage(toolExecutionRequest);
                        sink.next(JSONUtil.toJsonStr(toolRequestMessage));
                    })
                    /*
                     * 作用：处理 AI 模型返回的工具执行结果
                     * 触发时机：当 AI 模型调用工具时，会返回工具执行结果
                     * 用途：用于实现工具执行的实时反馈，让用户能够看到工具执行的进度和结果
                     */
                    .onToolExecuted((ToolExecution toolExecution) -> {
                        ToolExecutedMessage toolExecutedMessage = new ToolExecutedMessage(toolExecution);
                        sink.next(JSONUtil.toJsonStr(toolExecutedMessage));
                    })
                    /*
                     * 作用：处理 AI 模型返回的完整响应
                     * 触发时机：当 AI 模型返回完整响应时，会触发该方法
                     * 用途：用于实现完整响应的实时反馈，让用户能够看到 AI 模型返回的完整结果
                     */
                    .onCompleteResponse((ChatResponse response) -> {
                        sink.complete();
                        // 检查任务是否被取消再构建 Vue 项目
                        if (!taskCancellationManager.isTaskCancelled(appId)) {
                            // 异步构建 Vue 项目
                            String projectPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + "vue_project_"
                                    + appId;
                            vueProjectBuilder.buildProjectAsync(projectPath);
                            log.info("应用 {} Vue项目构建完成", appId);
                        } else {
                            log.info("应用 {} 的任务已被取消，跳过Vue项目构建", appId);
                        }
                    })
                    /*
                     * 作用：处理 AI 模型返回的错误信息
                     * 触发时机：当 AI 模型返回错误信息时，会触发该方法
                     * 用途：用于处理错误信息，让用户能够看到错误信息并采取相应的处理措施
                     */
                    .onError((Throwable e) -> {
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
}
