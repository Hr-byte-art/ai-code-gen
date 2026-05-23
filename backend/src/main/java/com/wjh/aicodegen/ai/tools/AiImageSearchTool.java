package com.wjh.aicodegen.ai.tools;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.wjh.aicodegen.langgraph4j.model.ImageResource;
import com.wjh.aicodegen.langgraph4j.model.enums.ImageCategoryEnum;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 王哈哈
 */
@Slf4j
@Component
public class AiImageSearchTool extends BaseTool {

    private static final String PEXELS_API_URL = "https://api.pexels.com/v1/search";

    @Value("${picture.search.api-key}")
    private String pictureSearchApiKey;

    @Tool("搜索内容相关的图片，用于网站内容展示")
    public List<ImageResource> searchContentImages(@P("搜索关键词") String query) {
        // 在工具执行前设置MonitorContext，确保Token统计正确
        setupMonitorContext();

        List<ImageResource> imageList = new ArrayList<>();

        // 参数验证
        if (query == null || query.trim().isEmpty()) {
            log.warn("图片搜索关键词为空，返回空列表");
            return ListUtil.empty();
        }

        // API Key 验证
        if (pictureSearchApiKey == null || pictureSearchApiKey.trim().isEmpty()) {
            log.error("Pexels API Key 未配置，无法搜索图片");
            return ListUtil.empty();
        }

        int searchCount = 6;
        String trimmedQuery = query.trim();

        try (HttpResponse response = HttpRequest.get(PEXELS_API_URL)
                .header("Authorization", pictureSearchApiKey)
                // GET 请求使用 form 方法是正确的，hutool 会自动转为查询参数
                .form("query", trimmedQuery)
                .form("per_page", String.valueOf(searchCount))
                .form("page", "1")
                .execute()) {

            if (response.isOk()) {
                String responseBody = response.body();
                if (responseBody == null || responseBody.trim().isEmpty()) {
                    log.warn("Pexels API 返回空响应");
                    return imageList;
                }

                JSONObject result = JSONUtil.parseObj(responseBody);
                JSONArray photos = result.getJSONArray("photos");

                if (photos == null || photos.isEmpty()) {
                    log.info("未找到关键词 '{}' 相关的图片", trimmedQuery);
                    return imageList;
                }

                for (int i = 0; i < photos.size(); i++) {
                    JSONObject photo = photos.getJSONObject(i);
                    if (photo == null)
                        continue;

                    JSONObject src = photo.getJSONObject("src");
                    if (src == null) {
                        log.warn("图片 {} 缺少 src 信息", i);
                        continue;
                    }

                    String imageUrl = src.getStr("medium");
                    if (imageUrl == null || imageUrl.trim().isEmpty()) {
                        log.warn("图片 {} 的 URL 为空", i);
                        continue;
                    }

                    imageList.add(ImageResource.builder()
                            .category(ImageCategoryEnum.CONTENT)
                            .description(photo.getStr("alt", trimmedQuery))
                            .url(imageUrl)
                            .build());
                }

                log.info("成功搜索到 {} 张图片，关键词: '{}'", imageList.size(), trimmedQuery);

            } else {
                log.error("Pexels API 调用失败，HTTP状态码: {}, 响应: {}",
                        response.getStatus(), response.body());
            }

        } catch (Exception e) {
            log.error("调用 Pexels API 时发生异常，关键词: '{}', 错误: {}", trimmedQuery, e.getMessage(), e);
        }

        return imageList;
    }

    @Override
    public String getToolName() {
        return "searchContentImages";
    }

    @Override
    public String getDisplayName() {
        return "搜索内容相关的图片";
    }

    @Override
    public String generateToolExecutedResult(JSONObject arguments) {
        // 图片搜索工具执行后只显示简洁的工具调用信息，不展示具体搜索内容
        return String.format("【工具调用】%s", getDisplayName());
    }
}
