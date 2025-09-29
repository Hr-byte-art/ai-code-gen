package com.wjh.aicodegen.config.ai;

import com.wjh.aicodegen.observability.AiTokenStatisticsListener;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import jakarta.annotation.Resource;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;

import java.util.List;

/**
 * @Author 王哈哈
 * @Date 2025/8/26 23:08:33
 * @Description 配置
 */
@Configuration
@ConfigurationProperties(prefix = "langchain4j.open-ai.generation-app-name-model")
@Data
public class GenAppNameAiModelConfig {

    /**
     * 模型基础url
     */
    private String baseUrl;

    /**
     * apiKey
     */
    private String apiKey;

    /**
     * 模型名称
     */
    private String modelName;

    /**
     * 日志请求
     */
    private Boolean logRequests = false;

    /**
     * 日志响应
     */
    private Boolean logResponses = false;

    @Resource
    private AiTokenStatisticsListener aiTokenStatisticsListener;

    /**
     * 创建生成AppName模型实例
     */
    @Bean
    @Scope("prototype")
    public ChatModel generationAppNameModelPrototype() {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .listeners(List.of(aiTokenStatisticsListener))
                .modelName(modelName)
                .baseUrl(baseUrl)
                .logRequests(logRequests)
                .logResponses(logResponses)
                .build();
    }
}
