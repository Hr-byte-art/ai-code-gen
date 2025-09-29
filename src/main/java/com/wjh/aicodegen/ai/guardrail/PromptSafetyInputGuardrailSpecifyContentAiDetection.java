package com.wjh.aicodegen.ai.guardrail;

import com.wjh.aicodegen.ai.factory.AiInputPromptDetectionServiceFactory;
import com.wjh.aicodegen.ai.service.AiInputPromptDetectionService;
import com.wjh.aicodegen.model.entity.AiDetection;
import com.wjh.aicodegen.model.enums.AiCallPurposeEnum;
import com.wjh.aicodegen.monitor.GlobalContextStorage;
import com.wjh.aicodegen.monitor.MonitorContext;
import com.wjh.aicodegen.monitor.MonitorContextHolder;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.guardrail.InputGuardrailResult;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @Author 王哈哈
 * @Date 2025/8/24 19:05:43
 * @Description 输入内容安全检测
 */
@Slf4j
@Component
public class PromptSafetyInputGuardrailSpecifyContentAiDetection implements InputGuardrail {

    @Resource
    private AiInputPromptDetectionServiceFactory aiInputPromptDetectionServiceFactory;

    @Override
    public InputGuardrailResult validate(UserMessage userMessage) {
        // 保存原始上下文，以便后续恢复
        MonitorContext originalContext = MonitorContextHolder.getContext();
        if (originalContext == null) {
            originalContext = GlobalContextStorage.getLatestContext();
        }

        try {
            // 【细粒度分类】为护轨机制设置安全检查上下文
            setupSafetyCheckContext();

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
            AiInputPromptDetectionService aiInputPromptDetectionService = aiInputPromptDetectionServiceFactory
                    .createInputPromptDetectionService();
            AiDetection aiDetection = aiInputPromptDetectionService.detectInputPrompt(lowerInput);
            if (!aiDetection.isSafe()) {
                return fatal("输入内容包含敏感词：" + aiDetection.getReason());
            }
            return success();
        } finally {
            // 恢复原始上下文，确保护轨机制不影响后续AI调用
            if (originalContext != null) {
                MonitorContextHolder.setContext(originalContext);
                log.info("护轨机制完成，恢复原始上下文: userId={}, appId={}, aiCallPurpose={}",
                        originalContext.getUserId(), originalContext.getAppId(), originalContext.getAiCallPurpose());
            } else {
                MonitorContextHolder.setContext(null);
                log.info("护轨机制完成，清除临时上下文");
            }
        }
    }

    /**
     * 为安全检查设置监控上下文
     */
    private void setupSafetyCheckContext() {
        // 尝试从现有上下文或全局存储获取上下文
        MonitorContext existingContext = MonitorContextHolder.getContext();

        if (existingContext == null) {
            existingContext = GlobalContextStorage.getLatestContext();
        }

        if (existingContext != null) {
            // 创建护轨专用的临时上下文副本，不影响原始上下文
            MonitorContext safetyContext = MonitorContext.builder()
                    .userId(existingContext.getUserId())
                    .appId(existingContext.getAppId())
                    .aiCallPurpose(AiCallPurposeEnum.INPUT_SAFETY_CHECK.getCode())
                    .build();
            MonitorContextHolder.setContext(safetyContext);
            log.info("护轨机制创建临时安全检查上下文: userId={}, appId={}, aiCallPurpose={}",
                    safetyContext.getUserId(), safetyContext.getAppId(), safetyContext.getAiCallPurpose());
        } else {
            // 创建默认的安全检查上下文
            MonitorContext safetyContext = MonitorContext.builder()
                    .userId("safety_check")
                    .appId("safety_check")
                    .aiCallPurpose(AiCallPurposeEnum.INPUT_SAFETY_CHECK.getCode())
                    .build();
            MonitorContextHolder.setContext(safetyContext);
            log.info("护轨机制创建默认安全检查上下文: aiCallPurpose={}",
                    AiCallPurposeEnum.INPUT_SAFETY_CHECK.getCode());
        }
    }
}
