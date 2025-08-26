package com.wjh.aicodegen.ai.factory;

import com.wjh.aicodegen.ai.service.AiGenerateAppNameService;
import com.wjh.aicodegen.manager.SpringContextUtil;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Author 王哈哈
 * @Date 2025/8/26 23:01:46
 * @Description
 */
@Configuration
@Slf4j
public class AiGenerateAppNameServiceFactory {

    /**
     * 创建 AiGenerateAppNameService
     *
     */
    public AiGenerateAppNameService createAiGenerateAppNameService(){
        // 动态获取多例的路由 ChatModel，支持并发
        ChatModel generateAppNameChatModelPrototype = SpringContextUtil.getBean("generationAppNameModelPrototype", ChatModel.class);
        return AiServices.builder(AiGenerateAppNameService.class)
                .chatModel(generateAppNameChatModelPrototype)
                .build();
    }

    @Bean
    public AiGenerateAppNameService aiGenerateAppNameService() {
        return createAiGenerateAppNameService();
    }
}
