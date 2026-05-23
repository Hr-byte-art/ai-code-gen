package com.wjh.aicodegen.langgraph4j.tools;

import cn.hutool.core.util.StrUtil;
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
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 木子宸
 */
@Slf4j
@Component
public class IgoutuIllustrationTool {

    private static final String UNDRAW_API_URL = "https://api-ouch.icons8.com/api/frontend/v2/illustrations/search?locale=zh-CN&page=1&per_page=%s&search=%s";

    @Tool("搜索插画图片，用于网站美化和装饰")
    public List<ImageResource> searchIllustrations(@P("搜索关键词") String query) {
        List<ImageResource> imageList = new ArrayList<>();
        int searchCount = 4;
        String apiUrl = String.format(UNDRAW_API_URL, searchCount, query);

        try (HttpResponse response = HttpRequest.get(apiUrl).timeout(10000).execute()) {
            if (!response.isOk()) {
                return imageList;
            }
            JSONObject result = JSONUtil.parseObj(response.body());
            Integer total = result.getInt("total");
            if (total == null || total <= 0 ) {
                return imageList;
            }
            JSONArray illustrations = result.getJSONArray("illustrations");
            if (illustrations == null){
                return imageList;
            }

            int actualCount = Math.min(searchCount, illustrations.size());
            for (int i = 0; i < actualCount; i++) {
                JSONObject illustration = illustrations.getJSONObject(i);
                String title = illustration.getStr("heading", "插画");
                JSONArray styles = illustration.getJSONArray("styles");
                String str = "";
                if (styles != null && !styles.isEmpty()) {
                    // 获取第一个 style 中的 icon url
                    JSONObject firstStyle = styles.getJSONObject(0);
                    JSONObject icon = firstStyle.getJSONObject("icon");
                    if (icon != null) {
                        str = icon.getStr("url", "");
                    }
                }
                if (StrUtil.isNotBlank(str)) {
                    imageList.add(ImageResource.builder()
                            .category(ImageCategoryEnum.ILLUSTRATION)
                            .description(title)
                            .url(str)
                            .build());
                }
            }
        } catch (Exception e) {
            log.error("搜索插画失败：{}", e.getMessage(), e);
        }

        return imageList;
    }
}

