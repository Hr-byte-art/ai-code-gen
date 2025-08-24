package com.wjh.aicodegen.ai.service;

import com.wjh.aicodegen.model.entity.AiDetection;
import dev.langchain4j.service.SystemMessage;

/**
 * @Author 王哈哈
 * @Date 2025/8/24 19:06:24
 * @Description 输入提示检测服务
 */
public interface AiInputPromptDetectionService {
    /**
     * 检测输入提示
     * @param userMessage
     * @return
     */
    @SystemMessage(fromResource = "prompt/input-prompt-detection-system-prompt.txt")
    AiDetection detectInputPrompt(String userMessage);
}
