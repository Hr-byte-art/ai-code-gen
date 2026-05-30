package com.wjh.aicodegen.ai.factory;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.wjh.aicodegen.ai.guardrail.PromptSafetyInputGuardrailSpecifyContentAiDetection;
import com.wjh.aicodegen.ai.service.AiCodeGeneratorService;
import com.wjh.aicodegen.ai.tools.*;
import com.wjh.aicodegen.ai.tools.CustomToolProvider;
import com.wjh.aicodegen.manager.SpringContextUtil;
import com.wjh.aicodegen.model.entity.CodeSkill;
import com.wjh.aicodegen.monitor.GlobalContextStorage;
import com.wjh.aicodegen.monitor.MonitorContext;
import com.wjh.aicodegen.monitor.MonitorContextHolder;
import com.wjh.aicodegen.service.ChatHistoryService;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * AI 代码生成服务工厂
 * 根据 CodeSkill 动态构建 AI 服务实例（模型、工具、system prompt）
 *
 * @author 王哈哈
 */
@Configuration
@Slf4j
public class AiCodeGeneratorServiceFactory {

    @Resource(name = "customChatModel")
    private ChatModel chatModel;

    @Resource
    private ChatMemoryStore redisChatMemoryStore;

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private ToolManager toolManager;

    private final Cache<String, AiCodeGeneratorService> serviceCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .expireAfterAccess(Duration.ofMinutes(10))
            .removalListener((key, value, cause) -> {
                log.debug("AI 服务实例被移除，缓存键: {}, 原因: {}", key, cause);
            })
            .build();

    /**
     * 根据 appId 和 CodeSkill 获取服务（带缓存）
     */
    public AiCodeGeneratorService getAiCodeGeneratorService(long appId, CodeSkill skill) {
        String cacheKey = appId + "_" + skill.getSkillKey();
        ensureMonitorContext(appId);
        return serviceCache.get(cacheKey, key -> createAiCodeGeneratorService(appId, skill));
    }

    /**
     * 根据 CodeSkill 动态创建 AI 服务实例
     */
    private AiCodeGeneratorService createAiCodeGeneratorService(long appId, CodeSkill skill) {
        // 构建对话记忆
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory
                .builder()
                .id(appId)
                .chatMemoryStore(redisChatMemoryStore)
                .maxMessages(20)
                .build();
        chatHistoryService.loadChatHistoryToMemory(appId, chatMemory, 4);

        // 解析工具列表
        Object[] tools = resolveTools(skill.getToolNames());

        // 根据 model_strategy 选择模型
        boolean useReasoning = "reasoning".equals(skill.getModelStrategy());

        // 构建 AI Service
        AiServices<AiCodeGeneratorService> builder = AiServices.builder(AiCodeGeneratorService.class);

        if (useReasoning) {
            StreamingChatModel reasoningModel = SpringContextUtil
                    .getBean("reasoningStreamingChatModelPrototype", StreamingChatModel.class);
            builder.streamingChatModel(reasoningModel)
                    .chatMemoryProvider(memoryId -> chatMemory);
        } else {
            StreamingChatModel standardModel = SpringContextUtil
                    .getBean("streamingChatModelPrototype", StreamingChatModel.class);
            builder.chatModel(chatModel)
                    .streamingChatModel(standardModel)
                    .chatMemoryProvider(memoryId -> chatMemory);
        }

        // 动态注入 system prompt
        builder.systemMessage(skill.getSystemPrompt());

        // 注入自定义工具（Skill 定义的 HTTP 工具）
        if (skill.getCustomTools() != null && !skill.getCustomTools().isBlank()) {
            CustomToolProvider customToolProvider = SpringContextUtil.getBean(CustomToolProvider.class);
            customToolProvider.registerTools(skill.getCustomTools());
            // 将 CustomToolProvider 加入工具列表
            Object[] toolsWithCustom = new Object[tools.length + 1];
            System.arraycopy(tools, 0, toolsWithCustom, 0, tools.length);
            toolsWithCustom[tools.length] = customToolProvider;
            tools = toolsWithCustom;
            log.info("注入自定义工具: skill={}, tools={}", skill.getSkillKey(), customToolProvider.getRegisteredToolNames());
        }

        // 注入工具
        if (tools.length > 0) {
            builder.tools(tools);
        }

        // 工具调用轮次上限（防止 AI 陷入无限循环）
        builder.maxToolCallingRoundTrips(200);

        // 输入护轨
        builder.inputGuardrails(
                SpringContextUtil.getBean(PromptSafetyInputGuardrailSpecifyContentAiDetection.class));

        // 工具名幻觉处理
        builder.hallucinatedToolNameStrategy(toolExecutionRequest ->
                ToolExecutionResultMessage.from(toolExecutionRequest,
                        "Error: there is no tool called " + toolExecutionRequest.name()));

        AiCodeGeneratorService service = builder.build();
        log.info("创建 AI 服务: skill={}, appId={}, model={}, tools={}",
                skill.getSkillKey(), appId, useReasoning ? "reasoning" : "standard",
                skill.getToolNames() != null ? skill.getToolNames() : "all");
        return service;
    }

    /**
     * 解析工具名称列表，返回对应的工具实例
     * null 或空表示使用代码生成默认工具集
     */
    private static final Set<String> DEFAULT_CODE_GENERATION_TOOLS = Set.of(
            "readDir",
            "readFile",
            "writeFile",
            "modifyFile",
            "deleteFile",
            "webSearch",
            "webFetch",
            "exit"
    );

    private Object[] resolveTools(String toolNames) {
        // 始终包含默认代码生成工具
        Set<String> allToolNames = new HashSet<>(DEFAULT_CODE_GENERATION_TOOLS);

        // 追加自定义工具
        if (toolNames != null && !toolNames.isBlank()) {
            for (String name : toolNames.split(",")) {
                String trimmed = name.trim();
                if (!trimmed.isBlank()) {
                    allToolNames.add(trimmed);
                }
            }
        }

        return allToolNames.stream()
                .map(name -> {
                    BaseTool tool = toolManager.getTool(name);
                    if (tool == null) {
                        log.warn("工具不存在: {}", name);
                    }
                    return tool;
                })
                .filter(t -> t != null)
                .toArray();
    }

    private void ensureMonitorContext(long appId) {
        MonitorContext existingContext = MonitorContextHolder.getContext();
        if (existingContext != null &&
                !existingContext.getUserId().equals("unknown") &&
                !existingContext.getAppId().equals("unknown")) {
            if (!"CODE_GENERATION".equals(existingContext.getAiCallPurpose())) {
                existingContext.setAiCallPurpose("CODE_GENERATION");
                MonitorContextHolder.setContext(existingContext);
            }
            // 存入全局存储，确保跨线程可访问
            GlobalContextStorage.storeContext(existingContext);
            return;
        }
        try {
            MonitorContext newContext = MonitorContext.builder()
                    .appId(String.valueOf(appId))
                    .userId("system")
                    .aiCallPurpose("CODE_GENERATION")
                    .build();
            MonitorContextHolder.setContext(newContext);
            // 存入全局存储，确保跨线程可访问
            GlobalContextStorage.storeContext(newContext);
        } catch (Exception e) {
            log.error("重建MonitorContext失败: appId={}, error={}", appId, e.getMessage());
        }
    }
}
