package com.wjh.aicodegen.core.handler;

import com.wjh.aicodegen.model.entity.User;
import com.wjh.aicodegen.service.ChatHistoryService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * 流处理器执行器
 * 根据 build_strategy 创建合适的流处理器：
 * 1. "none" → SimpleTextStreamHandler（HTML、MULTI_FILE 的纯文本流）
 * 2. "vue" / "fullstack" → JsonMessageStreamHandler（工具增强的 JSON 消息流）
 *
 * @author 王哈哈
 */
@Slf4j
@Component
public class StreamHandlerExecutor {

    @Resource
    private JsonMessageStreamHandler jsonMessageStreamHandler;

    @Resource
    private SimpleTextStreamHandler simpleTextStreamHandler;

    /**
     * 创建流处理器并处理聊天历史记录
     *
     * @param originFlux         原始流
     * @param chatHistoryService 聊天历史服务
     * @param appId              应用ID
     * @param loginUser          登录用户
     * @param buildStrategy      构建策略: none/vue/fullstack
     * @return 处理后的流
     */
    public Flux<String> doExecute(Flux<String> originFlux,
                                  ChatHistoryService chatHistoryService,
                                  long appId, User loginUser, String buildStrategy) {
        if ("none".equals(buildStrategy)) {
            return simpleTextStreamHandler.handle(originFlux, chatHistoryService, appId, loginUser);
        } else {
            return jsonMessageStreamHandler.handle(originFlux, chatHistoryService, appId, loginUser);
        }
    }
}
