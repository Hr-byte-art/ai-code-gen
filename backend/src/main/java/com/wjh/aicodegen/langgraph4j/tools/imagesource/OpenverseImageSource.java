package com.wjh.aicodegen.langgraph4j.tools.imagesource;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.wjh.aicodegen.langgraph4j.model.ImageResource;
import com.wjh.aicodegen.langgraph4j.model.enums.ImageCategoryEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Openverse 图片搜索源
 * WordPress 开放许可图片搜索引擎，聚合 7 亿+ 图片，无需 API Key
 * 只返回商用许可图片（CC0, CC-BY, CC-BY-SA, PDM）
 */
@Slf4j
@Component
public class OpenverseImageSource implements ImageSource {

    private static final String API_URL = "https://api.openverse.org/v1/images/";

    /** 商用友好的许可证类型 */
    private static final Set<String> COMMERCIAL_LICENSES = Set.of(
            "by",       // CC-BY
            "by-sa",    // CC-BY-SA
            "cc0",      // CC0 公共领域
            "pdm"       // Public Domain Mark
    );

    @Override
    public String getName() {
        return "openverse";
    }

    @Override
    public List<ImageResource> search(String query, int count) {
        if (query == null || query.isBlank()) {
            return Collections.emptyList();
        }

        List<ImageResource> imageList = new ArrayList<>();
        String trimmedQuery = query.trim();

        try {
            String encodedQuery = URLEncoder.encode(trimmedQuery, StandardCharsets.UTF_8);
            String url = String.format("%s?q=%s&page_size=%d&license=%s",
                    API_URL, encodedQuery, count, String.join(",", COMMERCIAL_LICENSES));

            HttpResponse response = HttpRequest.get(url)
                    .timeout(10000)
                    .header("User-Agent", "AiCodeGenPlatform/1.0")
                    .execute();

            if (response.isOk()) {
                String body = response.body();
                if (body == null || body.isBlank()) {
                    log.warn("Openverse API 返回空响应");
                    return imageList;
                }

                JSONObject result = JSONUtil.parseObj(body);
                JSONArray results = result.getJSONArray("results");
                if (results == null || results.isEmpty()) {
                    log.info("Openverse 未找到关键词 '{}' 相关的图片", trimmedQuery);
                    return imageList;
                }

                for (int i = 0; i < results.size(); i++) {
                    JSONObject item = results.getJSONObject(i);
                    if (item == null) continue;

                    String imageUrl = item.getStr("url");
                    if (imageUrl == null || imageUrl.isBlank()) continue;

                    // 优先用 thumbnail（更快），fallback 到原图
                    String displayUrl = item.getStr("thumbnail");
                    if (displayUrl == null || displayUrl.isBlank()) {
                        displayUrl = imageUrl;
                    }

                    String title = item.getStr("title");
                    if (title == null || title.isBlank()) {
                        title = trimmedQuery;
                    }

                    String creator = item.getStr("creator");
                    String license = item.getStr("license");
                    String licenseVersion = item.getStr("license_version");

                    // 构建描述：title + 来源信息
                    String description = title;
                    if (creator != null && !creator.isBlank()) {
                        description += " (by " + creator + ")";
                    }

                    imageList.add(ImageResource.builder()
                            .category(ImageCategoryEnum.CONTENT)
                            .description(description)
                            .url(displayUrl)
                            .build());
                }

                log.info("Openverse 搜索完成: keyword='{}', found={}", trimmedQuery, imageList.size());
            } else {
                log.error("Openverse API 调用失败，HTTP状态码: {}", response.getStatus());
            }
        } catch (Exception e) {
            log.error("Openverse API 调用异常: keyword='{}', error={}", trimmedQuery, e.getMessage());
        }

        return imageList;
    }
}
