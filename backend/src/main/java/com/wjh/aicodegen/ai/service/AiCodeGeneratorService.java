package com.wjh.aicodegen.ai.service;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import reactor.core.publisher.Flux;

/**
 * AI 代码生成服务接口
 * 通用接口，system prompt 由 Factory 在构建时动态注入
 *
 * @author 王哈哈
 */
public interface AiCodeGeneratorService {

    /**
     * 标准代码生成（HTML / 多文件）
     * 返回 Flux<String>，流式输出代码文本
     */
    Flux<String> generateCode(@UserMessage("{{userMessage}}") @V("userMessage") String userMessage);

    /**
     * 工具增强代码生成（Vue 工程 / 全栈）
     * 返回 TokenStream，支持工具调用回调
     */
    TokenStream generateCodeWithTools(@MemoryId long appId, @UserMessage("{{userMessage}}") @V("userMessage") String userMessage);
}
