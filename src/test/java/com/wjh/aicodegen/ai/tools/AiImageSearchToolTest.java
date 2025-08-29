package com.wjh.aicodegen.ai.tools;

import com.wjh.aicodegen.langgraph4j.model.ImageResource;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @Author 王哈哈
 * @Date 2025/8/28 19:10:53
 * @Description
 */
@SpringBootTest
class AiImageSearchToolTest {

    @Resource
    private AiImageSearchTool aiImageSearchTool;

    @Test
    void generateToolExecutedResult() {
        List<ImageResource> ai = aiImageSearchTool.searchContentImages("AI");
        System.out.println(ai);
    }
}
