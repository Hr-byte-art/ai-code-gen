package com.wjh.aicodegen.config.ai;

import com.wjh.aicodegen.observability.AiTokenStatisticsListener;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import jakarta.annotation.Resource;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.List;

/**
 * 默认ChatModel配置 - 确保所有AI调用都注册Token统计监听器
 * 
 * 这个配置解决了AiCodeGeneratorServiceFactory中使用的openAiChatModel
 * 没有注册AiTokenStatisticsListener的问题
 *
 * @author AI Assistant
 */
@Configuration
@ConfigurationProperties(prefix = "langchain4j.open-ai.chat-model")
@Data
public class DefaultChatModelConfig {

    private String baseUrl;

    private String apiKey;

    private String modelName;

    private Integer maxTokens;

    private Double temperature;

    private Boolean logRequests = false;

    private Boolean logResponses = false;

    @Resource
    private AiTokenStatisticsListener aiTokenStatisticsListener;

    /**
     * 创建默认的ChatModel，确保注册Token统计监听器
     * 使用@Primary确保这个Bean会被优先注入到AiCodeGeneratorServiceFactory
     */
    @Bean("customChatModel")
    @Primary
    public ChatModel customChatModel() {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .listeners(List.of(aiTokenStatisticsListener))
                .baseUrl(baseUrl)
                .maxTokens(maxTokens)
                .temperature(temperature)
                .logRequests(logRequests)
                .logResponses(logResponses)
                .build();
    }
}
