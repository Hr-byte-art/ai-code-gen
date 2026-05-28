package com.wjh.aicodegen.langgraph4j.tools.imagesource;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.wjh.aicodegen.langgraph4j.model.ImageResource;
import com.wjh.aicodegen.langgraph4j.model.enums.ImageCategoryEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Pexels 图片搜索源
 * 高质量库存照片，风景和人像类图片优势明显
 */
@Slf4j
@Component
public class PexelsImageSource implements ImageSource {

    private static final String PEXELS_API_URL = "https://api.pexels.com/v1/search";

    @Value("${picture.search.api-key:}")
    private String apiKey;

    @Override
    public String getName() {
        return "pexels";
    }

    @Override
    public List<ImageResource> search(String query, int count) {
        if (apiKey == null || apiKey.isBlank()) {
            log.debug("Pexels API Key 未配置，跳过搜索");
            return Collections.emptyList();
        }

        if (query == null || query.isBlank()) {
            return Collections.emptyList();
        }

        List<ImageResource> imageList = new ArrayList<>();
        String trimmedQuery = query.trim();

        try (HttpResponse response = HttpRequest.get(PEXELS_API_URL)
                .header("Authorization", apiKey)
                .form("query", trimmedQuery)
                .form("per_page", String.valueOf(count))
                .form("page", "1")
                .execute()) {

            if (response.isOk()) {
                String responseBody = response.body();
                if (responseBody == null || responseBody.isBlank()) {
                    log.warn("Pexels API 返回空响应");
                    return imageList;
                }

                JSONObject result = JSONUtil.parseObj(responseBody);
                JSONArray photos = result.getJSONArray("photos");
                if (photos == null || photos.isEmpty()) {
                    log.info("Pexels 未找到关键词 '{}' 相关的图片", trimmedQuery);
                    return imageList;
                }

                for (int i = 0; i < photos.size(); i++) {
                    JSONObject photo = photos.getJSONObject(i);
                    if (photo == null) continue;

                    JSONObject src = photo.getJSONObject("src");
                    if (src == null) continue;

                    String imageUrl = src.getStr("medium");
                    if (imageUrl == null || imageUrl.isBlank()) continue;

                    imageList.add(ImageResource.builder()
                            .category(ImageCategoryEnum.CONTENT)
                            .description(photo.getStr("alt", trimmedQuery))
                            .url(imageUrl)
                            .build());
                }

                log.info("Pexels 搜索完成: keyword='{}', found={}", trimmedQuery, imageList.size());
            } else {
                log.error("Pexels API 调用失败，HTTP状态码: {}", response.getStatus());
            }
        } catch (Exception e) {
            log.error("Pexels API 调用异常: keyword='{}', error={}", trimmedQuery, e.getMessage());
        }

        return imageList;
    }
}
