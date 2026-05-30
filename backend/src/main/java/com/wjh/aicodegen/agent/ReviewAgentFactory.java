package com.wjh.aicodegen.agent;

import com.wjh.aicodegen.observability.AiTokenStatisticsListener;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.util.List;

/**
 * 代码审查 Agent 工厂
 */
@Configuration
@ConfigurationProperties(prefix = "agent.review")
@Data
public class ReviewAgentFactory {

    private String baseUrl;
    private String apiKey;
    private String modelName;
    private Integer maxTokens;
    private Double temperature;

    @Resource
    @Lazy
    private AiTokenStatisticsListener aiTokenStatisticsListener;

    @Bean
    public ReviewAgent reviewAgent() {
        ChatModel model = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .maxTokens(maxTokens != null ? maxTokens : 4096)
                .temperature(temperature != null ? temperature : 0.3)
                .listeners(List.of(aiTokenStatisticsListener))
                .build();

        return AiServices.builder(ReviewAgent.class)
                .chatModel(model)
                .build();
    }
}
