package com.wjh.aicodegen.ai.service;

import dev.langchain4j.service.SystemMessage;
import org.springframework.stereotype.Service;

/**
 * @Author 王哈哈
 * @Date 2025/8/26 22:50:39
 * @Description
 */
public interface AiGenerateAppNameService {

    /**
     * 生成应用名称
     * @param userMessage 用户输入
     * @return 应用名称
     */
    @SystemMessage(fromResource = "prompt/app-name-generation-system-prompt.txt")
    String generateAppName(String userMessage);
}
