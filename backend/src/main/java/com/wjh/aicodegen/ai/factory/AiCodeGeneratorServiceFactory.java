package com.wjh.aicodegen.ai.factory;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.wjh.aicodegen.ai.guardrail.PromptSafetyInputGuardrailSpecifyContentAiDetection;
import com.wjh.aicodegen.ai.service.AiCodeGeneratorService;
import com.wjh.aicodegen.ai.tools.*;
import com.wjh.aicodegen.exception.BusinessException;
import com.wjh.aicodegen.exception.ErrorCode;
import com.wjh.aicodegen.manager.SpringContextUtil;
import com.wjh.aicodegen.model.enums.CodeGenTypeEnum;
import com.wjh.aicodegen.monitor.MonitorContext;
import com.wjh.aicodegen.monitor.MonitorContextHolder;
import com.wjh.aicodegen.service.ChatHistoryService;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * @author 木子宸
 */
@Configuration
@Slf4j
public class AiCodeGeneratorServiceFactory {

    @Resource(name = "customChatModel")
    private ChatModel chatModel;

    @Resource
    private RedisChatMemoryStore redisChatMemoryStore;

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private ToolManager toolManager;

    @Resource
    private AiImageSearchTool aiImageSearchTool;

    /**
     * AI 服务实例缓存
     * 缓存策略：
     * - 最大缓存 1000 个实例
     * - 写入后 30 分钟过期
     * - 访问后 10 分钟过期
     */
    private final Cache<String, AiCodeGeneratorService> serviceCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .expireAfterAccess(Duration.ofMinutes(10))
            .removalListener((key, value, cause) -> {
                log.debug("AI 服务实例被移除，缓存键: {}, 原因: {}", key, cause);
            })
            .build();

    /**
     * 根据 appId 获取服务（带缓存）这个方法是为了兼容历史逻辑
     */
    public AiCodeGeneratorService getAiCodeGeneratorService(long appId) {
        return getAiCodeGeneratorService(appId, CodeGenTypeEnum.HTML);
    }

    /**
     * 根据 appId 和代码生成类型获取服务（带缓存）
     */
    public AiCodeGeneratorService getAiCodeGeneratorService(long appId, CodeGenTypeEnum codeGenType) {
        String cacheKey = buildCacheKey(appId, codeGenType);

        // 在获取AI服务前，确保当前线程有正确的MonitorContext
        ensureMonitorContext(appId, codeGenType);

        // 返回实例，如果没有就返回createAiCodeGeneratorService方法创建的新的实例
        return serviceCache.get(cacheKey, key -> createAiCodeGeneratorService(appId, codeGenType));
    }

    /**
     * 构建缓存键
     */
    private String buildCacheKey(long appId, CodeGenTypeEnum codeGenType) {
        return appId + "_" + codeGenType.getValue();
    }

    /**
     * 创建新的 AI 服务实例
     */
    private AiCodeGeneratorService createAiCodeGeneratorService(long appId, CodeGenTypeEnum codeGenType) {
        // 根据 appId 构建独立的对话记忆
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory
                .builder()
                .id(appId)
                .chatMemoryStore(redisChatMemoryStore)
                .maxMessages(20)
                .build();
        // 从数据库加载历史对话到记忆中
        chatHistoryService.loadChatHistoryToMemory(appId, chatMemory, 4);
        // 根据代码生成类型选择不同的模型配置
        return switch (codeGenType) {
            case VUE_PROJECT -> {
                // 使用多例模式的 StreamingChatModel 解决并发问题
                StreamingChatModel reasoningStreamingChatModel = SpringContextUtil
                        .getBean("reasoningStreamingChatModelPrototype", StreamingChatModel.class);
                yield AiServices.builder(AiCodeGeneratorService.class)
                        .streamingChatModel(reasoningStreamingChatModel)
                        .chatMemoryProvider(memoryId -> chatMemory)
                        .tools((Object[]) toolManager.getAllTools())
                        // 添加输入护轨
                        .inputGuardrails(
                                SpringContextUtil.getBean(PromptSafetyInputGuardrailSpecifyContentAiDetection.class))
                        // .inputGuardrails(new PromptSafetyInputGuardrail())
                        .hallucinatedToolNameStrategy(toolExecutionRequest -> ToolExecutionResultMessage.from(
                                toolExecutionRequest, "Error: there is no tool called " + toolExecutionRequest.name()))
                        .build();
            }
            case HTML, MULTI_FILE -> {
                // 使用多例模式的 StreamingChatModel 解决并发问题
                StreamingChatModel openAiStreamingChatModel = SpringContextUtil.getBean("streamingChatModelPrototype",
                        StreamingChatModel.class);
                yield AiServices.builder(AiCodeGeneratorService.class)
                        .chatModel(chatModel)
                        .streamingChatModel(openAiStreamingChatModel)
                        .chatMemory(chatMemory)
                        .tools(aiImageSearchTool)
                        .inputGuardrails(
                                SpringContextUtil.getBean(PromptSafetyInputGuardrailSpecifyContentAiDetection.class))
                        .build();
            }
            default -> throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                    "不支持的代码生成类型: " + codeGenType.getValue());
        };

    }

    /**
     * 确保当前线程有正确的MonitorContext
     * 这是为了解决线程切换导致的上下文丢失问题
     */
    private void ensureMonitorContext(long appId, CodeGenTypeEnum codeGenType) {
        MonitorContext existingContext = MonitorContextHolder.getContext();

        if (existingContext != null &&
                !existingContext.getUserId().equals("unknown") &&
                !existingContext.getAppId().equals("unknown")) {
            // 已有有效的上下文，确保aiCallPurpose是最新的
            if (!"CODE_GENERATION".equals(existingContext.getAiCallPurpose())) {
                existingContext.setAiCallPurpose("CODE_GENERATION");
                MonitorContextHolder.setContext(existingContext);
                log.debug("更新MonitorContext的aiCallPurpose: appId={}, aiCallPurpose=CODE_GENERATION", appId);
            }
            return;
        }

        // 尝试通过appId重建上下文
        try {
            // 这里可以从数据库查询App信息来重建上下文
            // 但为了避免循环依赖，我们使用一个简化的默认上下文
            MonitorContext newContext = MonitorContext.builder()
                    .appId(String.valueOf(appId))
                    .userId("system") // 使用system作为兜底，避免unknown
                    .aiCallPurpose("CODE_GENERATION") // 设置AI调用用途
                    .build();

            MonitorContextHolder.setContext(newContext);
            log.warn("AI服务工厂重建MonitorContext: appId={}, aiCallPurpose=CODE_GENERATION, 请检查上下文传递", appId);

        } catch (Exception e) {
            log.error("重建MonitorContext失败: appId={}, error={}", appId, e.getMessage());
        }
    }

}
