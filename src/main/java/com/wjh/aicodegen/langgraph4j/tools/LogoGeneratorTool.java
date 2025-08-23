package com.wjh.aicodegen.langgraph4j.tools;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesis;
import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesisParam;
import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesisResult;
import com.wjh.aicodegen.langgraph4j.model.ImageResource;
import com.wjh.aicodegen.langgraph4j.model.enums.ImageCategoryEnum;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * logo生成工具(因为要钱，而且挺贵的，所以使用放回一张静态图片)
 * @author 木子宸
 */
@Slf4j
@Component
public class LogoGeneratorTool {

//    @Value("${dashscope.api-key:}")
//    private String dashScopeApiKey;
//
//    @Value("${dashscope.image-model:wan2.2-t2i-flash}")
//    private String imageModel;
//
//    @Tool("根据描述生成 Logo 设计图片，用于网站品牌标识")
//    public List<ImageResource> generateLogos(@P("Logo 设计描述，如名称、行业、风格等，尽量详细") String description) {
//        List<ImageResource> logoList = new ArrayList<>();
//        try {
//            // 构建 Logo 设计提示词
//            String logoPrompt = String.format("生成 Logo，Logo 中禁止包含任何文字！Logo 介绍：%s", description);
//            ImageSynthesisParam param = ImageSynthesisParam.builder()
//                    .apiKey(dashScopeApiKey)
//                    .model(imageModel)
//                    .prompt(logoPrompt)
//                    .size("512*512")
//                    // 生成 1 张足够，因为 AI 不知道哪张最好
//                    .n(1)
//                    .build();
//            ImageSynthesis imageSynthesis = new ImageSynthesis();
//            ImageSynthesisResult result = imageSynthesis.call(param);
//            if (result != null && result.getOutput() != null && result.getOutput().getResults() != null) {
//                List<Map<String, String>> results = result.getOutput().getResults();
//                for (Map<String, String> imageResult : results) {
//                    String imageUrl = imageResult.get("url");
//                    if (StrUtil.isNotBlank(imageUrl)) {
//                        logoList.add(ImageResource.builder()
//                                .category(ImageCategoryEnum.LOGO)
//                                .description(description)
//                                .url(imageUrl)
//                                .build());
//                    }
//                }
//            }
//        } catch (Exception e) {
//            log.error("生成 Logo 失败: {}", e.getMessage(), e);
//        }
//        return logoList;
//    }

    @Tool("根据描述生成 Logo 设计图片，用于网站品牌标识")
    public List<ImageResource> generateLogos(@P("Logo 设计描述，如名称、行业、风格等，尽量详细") String description) {
        List<ImageResource> logoList = new ArrayList<>();
        try {
            logoList.add(ImageResource.builder()
                    .category(ImageCategoryEnum.LOGO)
                    .description( description)
                    .url("https://ai-code-gen-1340059484.cos.ap-chengdu.myqcloud.com/screenshots/default/default_project_cover.jpg")
                    .build());
        } catch (Exception e) {
            log.error("生成 Logo 失败: {}", e.getMessage(), e);
        }
        return logoList;
    }
}
