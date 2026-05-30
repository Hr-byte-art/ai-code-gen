package com.wjh.aicodegen.agent;

import com.wjh.aicodegen.agent.model.ReviewResult;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * 代码审查 Agent
 * 负责审查生成的代码质量，发现潜在问题
 */
public interface ReviewAgent {

    @SystemMessage(fromResource = "prompt/review-agent-system-prompt.txt")
    ReviewResult reviewCode(@UserMessage String codeContent);
}
