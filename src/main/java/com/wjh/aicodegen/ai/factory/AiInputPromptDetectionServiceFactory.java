package com.wjh.aicodegen.ai.factory;

import com.wjh.aicodegen.ai.service.AiCodeGenTypeRoutingService;
import com.wjh.aicodegen.ai.service.AiInputPromptDetectionService;
import com.wjh.aicodegen.manager.SpringContextUtil;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Author 王哈哈
 * @Date 2025/8/24 19:12:19
 * @Description 输入提示检测服务工厂
 */
@Slf4j
@Configuration
public class AiInputPromptDetectionServiceFactory {

    /**
     * 创建AI代码生成类型路由服务实例
     */
    public AiInputPromptDetectionService createInputPromptDetectionService() {
        // 动态获取多例的路由 ChatModel，支持并发
        ChatModel chatModel = SpringContextUtil.getBean("detectionChatModelPrototype", ChatModel.class);
        return AiServices.builder(AiInputPromptDetectionService.class)
                .chatModel(chatModel)
                .build();
    }

    /**
     * 默认提供一个 Bean
     */
    @Bean
    public AiInputPromptDetectionService aiInputPromptDetectionService() {
        return createInputPromptDetectionService();
    }

}
