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

/**
 * @author 王哈哈
 */
@Configuration
@ConfigurationProperties(prefix = "langchain4j.open-ai.streaming-chat-model")
@Data
public class StreamingChatModelConfig {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(StreamingChatModelConfig.class);

    private String baseUrl;

    private String apiKey;

    private String modelName;

    private Integer maxTokens;

    private Double temperature;

    private Integer timeout = 300;

    /**
     * 纯文本/HTML 生成默认不设置 max_tokens，避免业务层主动截断完整页面。
     * 如需控成本，可在环境配置中显式设置 simple-max-tokens。
     */
    private Integer simpleMaxTokens;

    private Integer simpleTimeout = 240;

    private boolean logRequests;

    private boolean logResponses;

    @Resource(name = "enhancedAiModelMonitorListener")
    private EnhancedAiModelMonitorListener aiModelMonitorListener;

    @Resource
    private AiTokenStatisticsListener aiTokenStatisticsListener;

    @Bean
    @Scope("prototype")
    public StreamingChatModel streamingChatModelPrototype() {
        return createModel(maxTokens, timeout, "标准");
    }

    public StreamingChatModel simpleStreamingChatModel() {
        return createModel(resolveSimpleMaxTokens(), resolveSimpleTimeout(), "轻量");
    }

    private StreamingChatModel createModel(Integer modelMaxTokens, Integer modelTimeout, String profileName) {
        Integer safeTimeout = modelTimeout != null ? modelTimeout : 300;
        log.info("创建{}StreamingChatModel: baseUrl={}, modelName={}, maxTokens={}, timeout={}s",
                profileName, baseUrl, modelName, modelMaxTokens, safeTimeout);
        var builder = OpenAiStreamingChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .listeners(List.of(aiModelMonitorListener, aiTokenStatisticsListener))
                .temperature(temperature)
                .timeout(Duration.ofSeconds(safeTimeout))
                .logRequests(logRequests)
                .logResponses(logResponses);
        if (modelMaxTokens != null && modelMaxTokens > 0) {
            builder.maxTokens(modelMaxTokens);
        }
        return builder.build();
    }

    private Integer resolveSimpleMaxTokens() {
        if (simpleMaxTokens != null && simpleMaxTokens > 0) {
            return simpleMaxTokens;
        }
        return null;
    }

    private Integer resolveSimpleTimeout() {
        if (simpleTimeout != null && simpleTimeout > 0) {
            return simpleTimeout;
        }
        return timeout;
    }

}
