package com.wjh.aicodegen.config;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

/**
 * @Author 王哈哈
 * @Date 2025/8/24 19:14:03
 * @Description 检测配置
 */
@Configuration
@ConfigurationProperties(prefix = "langchain4j.open-ai.detection-chat-model")
@Data
public class DetectionAiModelConfig {

    private String baseUrl;

    private String apiKey;

    private String modelName;

    private Boolean logRequests = false;

    private Boolean logResponses = false;

    /**
     * 创建检测模型
     * @return ChatModel
     */
    @Bean
    @Scope("prototype")
    public ChatModel detectionChatModelPrototype() {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .baseUrl(baseUrl)
                .logRequests(logRequests)
                .logResponses(logResponses)
                .build();
    }
}
