package com.wjh.aicodegen.ai.factory;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.wjh.aicodegen.ai.guardrail.PromptSafetyInputGuardrailSpecifyContentAiDetection;
import com.wjh.aicodegen.ai.service.AiCodeGeneratorService;
import com.wjh.aicodegen.ai.tools.*;
import com.wjh.aicodegen.ai.tools.CustomToolProvider;
import com.wjh.aicodegen.config.ai.StreamingChatModelConfig;
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

    @Resource
    private StreamingChatModelConfig streamingChatModelConfig;

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
        // 缓存 key 需要包含 buildStrategy，因为不同策略的工具集不同
        String cacheKey = appId + "_" + skill.getSkillKey() + "_" + skill.getBuildStrategy();
        ensureMonitorContext(appId);
        if (skill.getBuildStrategy() != null && !"none".equals(skill.getBuildStrategy())) {
            return createAiCodeGeneratorService(appId, skill);
        }
        return serviceCache.get(cacheKey, key -> createAiCodeGeneratorService(appId, skill));
    }

    /**
     * 根据 CodeSkill 动态创建 AI 服务实例
     */
    private AiCodeGeneratorService createAiCodeGeneratorService(long appId, CodeSkill skill) {
        boolean toolMode = skill.getBuildStrategy() != null && !"none".equals(skill.getBuildStrategy());
        // 构建对话记忆。工具模式下必须保留更多消息，避免 tool_call / tool_result 被窗口截断后触发上游 messages 非法。
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory
                .builder()
                .id(appId)
                .chatMemoryStore(redisChatMemoryStore)
                .maxMessages(toolMode ? 80 : 20)
                .build();
        if (toolMode) {
            chatMemory.clear();
        } else {
            chatHistoryService.loadChatHistoryToMemory(appId, chatMemory, 4);
        }

        // 解析工具列表（buildStrategy=none 时不加载默认工具，减少 prompt 体积）
        Object[] tools = resolveTools(skill.getToolNames(), skill.getBuildStrategy());

        // 根据 model_strategy 选择模型
        boolean useReasoning = "reasoning".equals(skill.getModelStrategy());

        // 构建 AI Service
        AiServices<AiCodeGeneratorService> builder = AiServices.builder(AiCodeGeneratorService.class);

        boolean simpleTextGeneration = "none".equals(skill.getBuildStrategy());

        if (useReasoning) {
            StreamingChatModel reasoningModel = SpringContextUtil
                    .getBean("reasoningStreamingChatModelPrototype", StreamingChatModel.class);
            builder.streamingChatModel(reasoningModel)
                    .chatMemoryProvider(memoryId -> chatMemory);
            log.info("使用推理模型: appId={}, skill={}", appId, skill.getSkillKey());
        } else {
            StreamingChatModel standardModel = simpleTextGeneration
                    ? streamingChatModelConfig.simpleStreamingChatModel()
                    : SpringContextUtil.getBean("streamingChatModelPrototype", StreamingChatModel.class);
            builder.chatModel(chatModel)
                    .streamingChatModel(standardModel)
                    .chatMemoryProvider(memoryId -> chatMemory);
            log.info("使用标准模型: appId={}, skill={}, profile={}",
                    appId, skill.getSkillKey(), simpleTextGeneration ? "simple" : "standard");
        }

        // 动态注入 system prompt
        String systemPrompt = skill.getSystemPrompt();
        builder.systemMessage(systemPrompt);
        log.info("System prompt长度: appId={}, 长度={}", appId, systemPrompt != null ? systemPrompt.length() : 0);

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
            log.info("注入 {} 个工具: {}", tools.length,
                    Arrays.stream(tools).map(t -> ((BaseTool) t).getToolName()).collect(java.util.stream.Collectors.joining(", ")));
        } else {
            log.info("无工具注入（纯文本生成模式）");
        }

        // 工具调用轮次上限（防止 AI 陷入无限循环）
        builder.maxToolCallingRoundTrips(resolveMaxToolRoundTrips(skill.getBuildStrategy()));

        // 输入护轨
        builder.inputGuardrails(
                SpringContextUtil.getBean(PromptSafetyInputGuardrailSpecifyContentAiDetection.class));

        // 工具名幻觉处理
        builder.hallucinatedToolNameStrategy(toolExecutionRequest ->
                ToolExecutionResultMessage.from(toolExecutionRequest,
                        "Error: there is no tool called " + toolExecutionRequest.name()));

        AiCodeGeneratorService service = builder.build();
        log.info("创建 AI 服务完成: skill={}, appId={}, model={}, 实际注入工具数={}",
                skill.getSkillKey(), appId, useReasoning ? "reasoning" : "standard", tools.length);
        return service;
    }

    private int resolveMaxToolRoundTrips(String buildStrategy) {
        if ("fullstack".equals(buildStrategy)) {
            return 90;
        }
        if ("vue".equals(buildStrategy) || "react".equals(buildStrategy) || "nextjs".equals(buildStrategy)) {
            return 55;
        }
        if ("auto".equals(buildStrategy)) {
            return 80;
        }
        return 20;
    }

    /**
     * 解析工具名称列表，返回对应的工具实例
     * buildStrategy=none 时不加载默认工具（HTML/多文件等纯文本生成模式）
     * 其他模式自动加载默认工具集，同时追加 Skill 显式指定的额外工具
     */
    private static final Set<String> DEFAULT_CODE_GENERATION_TOOLS = Set.of(
            "readDir",
            "readFile",
            "writeFile",
            "modifyFile",
            "webSearch",
            "webFetch",
            "exit"
    );

    private Object[] resolveTools(String toolNames, String buildStrategy) {
        Set<String> allToolNames = new HashSet<>();

        // 只有需要工具的模式才加载默认工具集，buildStrategy=none 表示纯文本生成（HTML/多文件），不需要工具
        if (buildStrategy != null && !"none".equals(buildStrategy)) {
            allToolNames.addAll(DEFAULT_CODE_GENERATION_TOOLS);
        }

        // 追加 Skill 显式指定的工具
        if (toolNames != null && !toolNames.isBlank()) {
            for (String name : toolNames.split(",")) {
                String trimmed = name.trim();
                if (!trimmed.isBlank()) {
                    allToolNames.add(trimmed);
                }
            }
        }

        if (allToolNames.isEmpty()) {
            log.info("无工具需要加载: buildStrategy={}", buildStrategy);
            return new Object[0];
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
