package com.wjh.aicodegen.langgraph4j.node;

import com.wjh.aicodegen.ai.factory.AiCodeGenTypeRoutingServiceFactory;
import com.wjh.aicodegen.ai.service.AiCodeGenTypeRoutingService;
import com.wjh.aicodegen.langgraph4j.state.WorkflowContext;
import com.wjh.aicodegen.manager.SpringContextUtil;
import com.wjh.aicodegen.model.enums.CodeGenTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * @author 木子宸
 */
@Slf4j
public class RouterNode {

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("执行节点: 智能路由");

            CodeGenTypeEnum generationType;
            try {
                // 获取AI路由服务
                AiCodeGenTypeRoutingServiceFactory routingServiceFactory =  SpringContextUtil.getBean(AiCodeGenTypeRoutingServiceFactory.class);
                AiCodeGenTypeRoutingService aiCodeGenTypeRoutingService = routingServiceFactory.createAiCodeGenTypeRoutingService();
                // 根据原始提示词进行智能路由
                generationType = aiCodeGenTypeRoutingService.routeCodeGenType(context.getOriginalPrompt());
                log.info("AI智能路由完成，选择类型: {} ({})", generationType.getValue(), generationType.getText());
            } catch (Exception e) {
                log.error("AI智能路由失败，使用默认HTML类型: {}", e.getMessage());
                generationType = CodeGenTypeEnum.HTML;
            }

            // 更新状态
            context.setCurrentStep("智能路由");
            context.setGenerationType(generationType);
            return WorkflowContext.saveContext(context);
        });
    }
}
