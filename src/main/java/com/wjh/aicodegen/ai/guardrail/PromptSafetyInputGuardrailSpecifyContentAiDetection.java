package com.wjh.aicodegen.ai.guardrail;

import com.wjh.aicodegen.ai.factory.AiInputPromptDetectionServiceFactory;
import com.wjh.aicodegen.ai.service.AiInputPromptDetectionService;
import com.wjh.aicodegen.model.entity.AiDetection;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.guardrail.InputGuardrailResult;
import jakarta.annotation.Resource;

import java.util.regex.Pattern;

/**
 * @Author 王哈哈
 * @Date 2025/8/24 19:05:43
 * @Description 输入内容安全检测
 */
public class PromptSafetyInputGuardrailSpecifyContentAiDetection implements InputGuardrail {

    @Resource
    private AiInputPromptDetectionServiceFactory aiInputPromptDetectionServiceFactory;

    @Override
    public InputGuardrailResult validate(UserMessage userMessage) {
        String input = userMessage.singleText();
        // 检查输入长度
        if (input.length() > 1000) {
            return fatal("输入内容过长，不要超过 1000 字");
        }
        // 检查是否为空
        if (input.trim().isEmpty()) {
            return fatal("输入内容不能为空");
        }

        // 使用Ai检查敏感词
        String lowerInput = input.toLowerCase();
        AiInputPromptDetectionService aiInputPromptDetectionService = aiInputPromptDetectionServiceFactory.createInputPromptDetectionService();
        AiDetection aiDetection = aiInputPromptDetectionService.detectInputPrompt(lowerInput);
        if (!aiDetection.isSafe()) {
            return fatal("输入内容包含敏感词：" + aiDetection.getReason());
        }
        return success();
    }
}
