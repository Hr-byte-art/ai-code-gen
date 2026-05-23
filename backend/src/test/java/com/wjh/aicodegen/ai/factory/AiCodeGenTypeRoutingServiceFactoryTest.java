package com.wjh.aicodegen.ai.factory;

import com.wjh.aicodegen.ai.service.AiCodeGenTypeRoutingService;
import com.wjh.aicodegen.model.enums.CodeGenTypeEnum;
import dev.langchain4j.agent.tool.P;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @Author 王哈哈
 * @Date 2025/8/24 19:47:22
 * @Description
 */
@SpringBootTest
@Slf4j
class AiCodeGenTypeRoutingServiceFactoryTest {

    @Resource
    private AiCodeGenTypeRoutingServiceFactory aiCodeGenTypeRoutingServiceFactory;

    @Test
    public void testCreateAiCodeGenTypeRoutingService() throws InterruptedException {


        String[] prompts = {
                "生成一个简单的 个人博客",
                "生成一个简单的 聊天机器人",
                "生成一个简单的 音乐播放器",
                "生成一个简单的 个人官网",
                "生成一个个人简介"
        };

        // 使用虚拟线程并发执行
        Thread[] threads = new Thread[prompts.length];
        for (int i = 0; i < prompts.length; i++) {
            final String prompt = prompts[i];
            final int index = i + 1;
            threads[i] = Thread.ofVirtual().start(() -> {
                AiCodeGenTypeRoutingService service = aiCodeGenTypeRoutingServiceFactory.createAiCodeGenTypeRoutingService();
                var result = service.routeCodeGenType(prompt);
                log.info("线程 {}: {} -> {}", index, prompt, result.getValue());
            });
        }

        for (Thread thread : threads) {
            thread.join();
        }

    }
}