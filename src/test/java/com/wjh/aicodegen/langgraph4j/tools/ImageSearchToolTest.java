package com.wjh.aicodegen.langgraph4j.tools;

import com.wjh.aicodegen.langgraph4j.model.ImageResource;
import com.wjh.aicodegen.langgraph4j.model.enums.ImageCategoryEnum;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @Author 王哈哈
 * @Date 2025/8/23 00:23:07
 * @Description
 */
@SpringBootTest
class ImageSearchToolTest {

    @Resource
    private ImageSearchTool imageSearchTool;

    @Test
    void testSearchContentImages() {
        // 测试正常搜索
        List<ImageResource> images = imageSearchTool.searchContentImages("computer");
        assertNotNull(images);
        assertFalse(images.isEmpty());
        // 验证返回的图片资源
        ImageResource firstImage = images.get(0);
        assertEquals(ImageCategoryEnum.CONTENT, firstImage.getCategory());
        assertNotNull(firstImage.getDescription());
        assertNotNull(firstImage.getUrl());
        assertTrue(firstImage.getUrl().startsWith("http"));
        System.out.println("搜索到 " + images.size() + " 张图片");
        images.forEach(image ->
                System.out.println("图片: " + image.getDescription() + " - " + image.getUrl())
        );
    }
}
