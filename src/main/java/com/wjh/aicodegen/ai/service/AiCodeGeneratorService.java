package com.wjh.aicodegen.ai.service;

import com.wjh.aicodegen.ai.model.HtmlCodeResult;
import com.wjh.aicodegen.ai.model.MultiFileCodeResult;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import reactor.core.publisher.Flux;

/**
 * @author 木子宸
 */
public interface AiCodeGeneratorService {



    /**
     * 生成 HTML 代码
     * @param userMessage 用户输入(流式)
     * @return  生成的代码
     */
    @SystemMessage(fromResource = "prompt/codegen-html-system-prompt.txt")
    Flux<String> generateHtmlCodeStream(String userMessage);

    /**
     * 生成多文件代码
     * @param userMessage 用户输入(流式)
     * @return  生成的代码
     */
    @SystemMessage(fromResource = "prompt/codegen-multi-file-system-prompt.txt")
    Flux<String> generateMultiFileCodeStream(String userMessage);

    /**
     * 生成 Vue 项目代码（流式）
     *
     * @param userMessage 用户消息
     * @return 生成过程的流式响应
     */
    @SystemMessage(fromResource = "prompt/codegen-vue-project-system-prompt.txt")
    TokenStream generateVueProjectCodeStream(@MemoryId long appId, @UserMessage String userMessage);


//    /**
//     * 生成 HTML 代码
//     * @param userMessage 用户输入
//     * @return  生成的代码
//     */
//    @SystemMessage(fromResource = "prompt/codegen-html-system-prompt.txt")
//    HtmlCodeResult generateHtmlCode(String userMessage);
//
//    /**
//     * 生成多文件代码
//     * @param userMessage 用户输入
//     * @return  生成的代码
//     */
//    @SystemMessage(fromResource = "prompt/codegen-multi-file-system-prompt.txt")
//    MultiFileCodeResult generateMultiFileCode(String userMessage);
}
