package com.wjh.aicodegen.core;

import cn.hutool.json.JSONUtil;
import com.wjh.aicodegen.ai.factory.AiCodeGeneratorServiceFactory;
import com.wjh.aicodegen.ai.model.HtmlCodeResult;
import com.wjh.aicodegen.ai.model.MultiFileCodeResult;
import com.wjh.aicodegen.ai.model.message.AiResponseMessage;
import com.wjh.aicodegen.ai.model.message.ToolExecutedMessage;
import com.wjh.aicodegen.ai.model.message.ToolRequestMessage;
import com.wjh.aicodegen.ai.service.AiCodeGeneratorService;
import com.wjh.aicodegen.core.parser.CodeParserExecutor;
import com.wjh.aicodegen.core.saver.CodeFileSaverExecutor;
import com.wjh.aicodegen.exception.BusinessException;
import com.wjh.aicodegen.exception.ErrorCode;
import com.wjh.aicodegen.model.enums.CodeGenTypeEnum;
import com.wjh.aicodegen.utils.ResultUtils;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.ToolExecution;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;

/**
 * AI 代码生成外观类，组合生成和保存功能
 * @author 木子宸
 */
@Service
@Slf4j
public class AiCodeGeneratorFacade {

    @Resource
    private AiCodeGeneratorServiceFactory aiCodeGeneratorServiceFactory;




    /**
     * 统一入口：根据类型生成并保存代码（流式，使用 appId）
     *
     * @param userMessage     用户提示词
     * @param codeGenTypeEnum 生成类型
     * @param appId           应用 ID
     */
    public Flux<String> generateAndSaveCodeStream(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId) {
// 根据 appId 获取对应的 AI 服务实例
        AiCodeGeneratorService aiCodeGeneratorService = aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(appId, codeGenTypeEnum);
        return switch (codeGenTypeEnum) {
            case HTML -> {
                Flux<String> codeStream = aiCodeGeneratorService.generateHtmlCodeStream(userMessage);
                yield processCodeStream(codeStream, CodeGenTypeEnum.HTML, appId);
            }
            case MULTI_FILE -> {
                Flux<String> codeStream = aiCodeGeneratorService.generateMultiFileCodeStream(userMessage);
                yield processCodeStream(codeStream, CodeGenTypeEnum.MULTI_FILE, appId);
            }
            case VUE_PROJECT -> {
                TokenStream codeStream = aiCodeGeneratorService.generateVueProjectCodeStream(appId, userMessage);
                yield processTokenStream(codeStream);
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
     * @return 流式响应
     */
    private Flux<String> processCodeStream(Flux<String> codeStream, CodeGenTypeEnum codeGenType, Long appId) {
        StringBuilder codeBuilder = new StringBuilder();
        // 实时收集代码片段
        return codeStream.doOnNext(codeBuilder::append).doOnComplete(() -> {
            // 流式返回完成后保存代码
            try {
                String completeCode = codeBuilder.toString();
                // 使用执行器解析代码
                Object parsedResult = CodeParserExecutor.executeParser(completeCode, codeGenType);
                // 使用执行器保存代码
                File savedDir = CodeFileSaverExecutor.executeSaver(parsedResult, codeGenType, appId);
                log.info("保存成功，路径为：" + savedDir.getAbsolutePath());
            } catch (Exception e) {
                log.error("保存失败: {}", e.getMessage());
            }
        });
    }

    /**
     * 将 TokenStream 转换为 Flux<String>，并传递工具调用信息
     *
     * @param tokenStream TokenStream 对象
     * @return Flux<String> 流式响应
     */
    private Flux<String> processTokenStream(TokenStream tokenStream) {
        return Flux.create(sink -> {

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
                      作用：处理 AI 模型返回的工具调用信息
                      触发时机：当 AI 模型需要调用工具时，会返回工具调用信息添加 事件处理的作用 的注释
                      用途：用于实现工具调用的实时反馈，让用户能够看到工具调用的进度和结果
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
                    })
                    /*
                     * 作用：处理 AI 模型返回的错误信息
                     * 触发时机：当 AI 模型返回错误信息时，会触发该方法
                     * 用途：用于处理错误信息，让用户能够看到错误信息并采取相应的处理措施
                     */
                    .onError((Throwable e) -> {
                        e.printStackTrace();
                        sink.error(e);
                    })
                    .start();
        });
    }

//    /**
//     * 统一入口：根据类型生成并保存代码（使用 appId）
//     * @param userMessage     用户提示词
//     * @param codeGenTypeEnum 生成类型
//     * @return 保存的目录
//     */
//    public File generateAndSaveCode(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId) {
//        // 根据 appId 获取对应的 AI 服务实例
//        AiCodeGeneratorService aiCodeGeneratorService = aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(appId);
//        if (codeGenTypeEnum == null) {
//            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成类型为空");
//        }
//        return switch (codeGenTypeEnum) {
//            case HTML -> {
//                HtmlCodeResult result = aiCodeGeneratorService.generateHtmlCode(userMessage);
//                yield CodeFileSaverExecutor.executeSaver(result, CodeGenTypeEnum.HTML, appId);
//            }
//            case MULTI_FILE -> {
//                MultiFileCodeResult result = aiCodeGeneratorService.generateMultiFileCode(userMessage);
//                yield CodeFileSaverExecutor.executeSaver(result, CodeGenTypeEnum.MULTI_FILE, appId);
//            }
//            default -> {
//                String errorMessage = "不支持的生成类型：" + codeGenTypeEnum.getValue();
//                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
//            }
//        };
//    }
//

    //    /**
//     * 统一入口：根据类型生成并保存代码
//     *
//     * @param userMessage     用户提示词
//     * @param codeGenTypeEnum 生成类型
//     * @return 保存的目录
//     */
//    public File generateAndSaveCode(String userMessage, CodeGenTypeEnum codeGenTypeEnum) {
//        if (codeGenTypeEnum == null) {
//            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成类型为空");
//        }
//        return switch (codeGenTypeEnum) {
//            case HTML -> generateAndSaveHtmlCode(userMessage);
//            case MULTI_FILE -> generateAndSaveMultiFileCode(userMessage);
//            default -> {
//                String errorMessage = "不支持的生成类型：" + codeGenTypeEnum.getValue();
//                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
//            }
//        };
//    }
//
//    public Flux<String> generateAndSaveCodeStream(String userMessage, CodeGenTypeEnum codeGenTypeEnum) {
//        if (codeGenTypeEnum == null) {
//            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成类型为空");
//        }
//        return switch (codeGenTypeEnum) {
//            case HTML -> generateAndSaveHtmlCodeStream(userMessage);
//            case MULTI_FILE -> generateMultiFileCodeStream(userMessage);
//            default -> {
//                String errorMessage = "不支持的生成类型：" + codeGenTypeEnum.getValue();
//                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
//            }
//        };
//    }

//    /**
//     * 生成 HTML 模式的代码并保存
//     *
//     * @param userMessage 用户提示词
//     * @return 保存的目录
//     */
//    private File generateAndSaveHtmlCode(String userMessage) {
//        HtmlCodeResult result = aiCodeGeneratorService.generateHtmlCode(userMessage);
//        return CodeFileSaver.saveHtmlCodeResult(result);
//    }
//
//    /**
//     * 生成多文件模式的代码并保存
//     *
//     * @param userMessage 用户提示词
//     * @return 保存的目录
//     */
//    private File generateAndSaveMultiFileCode(String userMessage) {
//        MultiFileCodeResult result = aiCodeGeneratorService.generateMultiFileCode(userMessage);
//        return CodeFileSaver.saveMultiFileCodeResult(result);
//    }
//
//    /**
//     * 生成html代码并保存 (响应流式)
//     *
//     * @param userMessage 用户提示词
//     * @return 保存的目录
//     */
//    private Flux<String> generateAndSaveHtmlCodeStream(String userMessage) {
//        Flux<String> result = aiCodeGeneratorService.generateMultiFileCodeStream(userMessage);
//        // 当流式返回结果,生成代码完成后,再保存代码
//        StringBuilder stringBuilder = new StringBuilder();
//        // 实时收集代码片段
//        return result.doOnNext(stringBuilder::append)
//                .doOnComplete(() -> {
//                    // 流式返回完成后保存代码
//                    try {
//                        String code = stringBuilder.toString();
//                        HtmlCodeResult htmlCodeResult = CodeParser.parseHtmlCode(code);
//                        // 保存代码到文件
//                        File savedDir = CodeFileSaver.saveHtmlCodeResult(htmlCodeResult);
//                        log.info("保存代码到文件：{}", savedDir.getAbsolutePath());
//                    } catch (Exception e) {
//                        log.error("保存代码出错：{}", e.getMessage());
//                    }
//                });
//    }
//
//    /**
//     * 生成多文件代码(响应流式)
//     *
//     * @param userMessage 用户消息
//     * @return  生成的多文件代码
//     */
//    public Flux<String> generateMultiFileCodeStream(String userMessage) {
//        Flux<String> codeStream = aiCodeGeneratorService.generateMultiFileCodeStream(userMessage);
//        // 响应流返回代码生成完成后,再保存代码
//        StringBuilder codeBuffer = new StringBuilder();
//        return codeStream
//                .doOnNext(codeBuffer::append)
//                .doOnComplete(() -> {
//                    try {
//                        MultiFileCodeResult multiFileCodeResult = CodeParser.parseMultiFileCode(codeBuffer.toString());
//                        // 保存代码到文件
//                        File codeFile = CodeFileSaver.saveMultiFileCodeResult(multiFileCodeResult);
//                        log.info("代码保存成功: {}", codeFile.getAbsolutePath());
//                    } catch (Exception e) {
//                        log.error("代码保存失败: {}", e.getMessage());
//                    }
//                });
//    }
//
//
//
//    /**
//     * 统一入口：根据类型生成并保存代码
//     *
//     * @param userMessage     用户提示词
//     * @param codeGenTypeEnum 生成类型
//     * @return 保存的目录
//     */
//    public File generateAndSaveCode(String userMessage, CodeGenTypeEnum codeGenTypeEnum) {
//        if (codeGenTypeEnum == null) {
//            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成类型为空");
//        }
//        return switch (codeGenTypeEnum) {
//            case HTML -> {
//                HtmlCodeResult result = aiCodeGeneratorService.generateHtmlCode(userMessage);
//                yield CodeFileSaverExecutor.executeSaver(result, CodeGenTypeEnum.HTML);
//            }
//            case MULTI_FILE -> {
//                MultiFileCodeResult result = aiCodeGeneratorService.generateMultiFileCode(userMessage);
//                yield CodeFileSaverExecutor.executeSaver(result, CodeGenTypeEnum.MULTI_FILE);
//            }
//            default -> {
//                String errorMessage = "不支持的生成类型：" + codeGenTypeEnum.getValue();
//                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
//            }
//        };
//    }
//
//    /**
//     * 统一入口：根据类型生成并保存代码（流式）
//     *
//     * @param userMessage     用户提示词
//     * @param codeGenTypeEnum 生成类型
//     */
//    public Flux<String> generateAndSaveCodeStream(String userMessage, CodeGenTypeEnum codeGenTypeEnum) {
//        if (codeGenTypeEnum == null) {
//            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成类型为空");
//        }
//        return switch (codeGenTypeEnum) {
//            case HTML -> {
//                Flux<String> codeStream = aiCodeGeneratorService.generateHtmlCodeStream(userMessage);
//                yield processCodeStream(codeStream, CodeGenTypeEnum.HTML);
//            }
//            case MULTI_FILE -> {
//                Flux<String> codeStream = aiCodeGeneratorService.generateMultiFileCodeStream(userMessage);
//                yield processCodeStream(codeStream, CodeGenTypeEnum.MULTI_FILE);
//            }
//            default -> {
//                String errorMessage = "不支持的生成类型：" + codeGenTypeEnum.getValue();
//                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
//            }
//        };
//    }
//
//
//    /**
//     * 通用流式代码处理方法
//     *
//     * @param codeStream  代码流
//     * @param codeGenType 代码生成类型
//     * @return 流式响应
//     */
//    private Flux<String> processCodeStream(Flux<String> codeStream, CodeGenTypeEnum codeGenType) {
//        StringBuilder codeBuilder = new StringBuilder();
//        // 实时收集代码片段
//        return codeStream.doOnNext(codeBuilder::append).doOnComplete(() -> {
//            // 流式返回完成后保存代码
//            try {
//                String completeCode = codeBuilder.toString();
//                // 使用执行器解析代码
//                Object parsedResult = CodeParserExecutor.executeParser(completeCode, codeGenType);
//                // 使用执行器保存代码
//                File savedDir = CodeFileSaverExecutor.executeSaver(parsedResult, codeGenType);
//                log.info("保存成功，路径为：" + savedDir.getAbsolutePath());
//            } catch (Exception e) {
//                log.error("保存失败: {}", e.getMessage());
//            }
//        });
//    }
}
