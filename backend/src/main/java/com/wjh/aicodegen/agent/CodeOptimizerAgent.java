package com.wjh.aicodegen.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * 代码优化 Agent
 * 根据审查结果优化代码
 */
public interface CodeOptimizerAgent {

    @SystemMessage(fromResource = "prompt/code-optimizer-agent-system-prompt.txt")
    String optimizeCode(@UserMessage String prompt);
}
