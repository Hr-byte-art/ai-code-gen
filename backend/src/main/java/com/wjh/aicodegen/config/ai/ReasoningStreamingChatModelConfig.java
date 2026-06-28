package com.wjh.aicodegen.config.ai;

import com.wjh.aicodegen.monitor.EnhancedAiModelMonitorListener;
import com.wjh.aicodegen.observability.AiTokenStatisticsListener;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import jakarta.annotation.Resource;
import lombok.Data;
import org.slf4j.Logger;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.time.Duration;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "langchain4j.open-ai.reasoning-streaming-chat-model")
@Data
public class ReasoningStreamingChatModelConfig {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(ReasoningStreamingChatModelConfig.class);

    private String baseUrl;

    private String apiKey;

    private String modelName;

    private Integer maxTokens;

    private Double temperature;

    private Integer timeout = 300; // 超时时间（秒），默认5分钟

    private Boolean logRequests = false;

    private Boolean logResponses = false;

    @Resource(name = "enhancedAiModelMonitorListener")
    private EnhancedAiModelMonitorListener aiModelMonitorListener;

    @Resource
    private AiTokenStatisticsListener aiTokenStatisticsListener;

    @Bean
    @Scope("prototype")
    public StreamingChatModel reasoningStreamingChatModelPrototype() {
        log.info("创建推理StreamingChatModel: baseUrl={}, modelName={}, maxTokens={}, timeout={}s",
                baseUrl, modelName, maxTokens, timeout);
        return OpenAiStreamingChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .maxTokens(maxTokens)
                .listeners(List.of(aiModelMonitorListener, aiTokenStatisticsListener))
                .temperature(temperature)
                .timeout(Duration.ofSeconds(timeout))
                .logRequests(logRequests)
                .logResponses(logResponses)
                .build();
    }
}
