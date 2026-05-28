package com.wjh.aicodegen.ai.tools;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 模拟 API 数据工具
 * 使用 JSONPlaceholder API（无需认证）
 */
@Slf4j
@Component
public class MockApiTool extends PublicApiTool {

    private static final String API = "https://jsonplaceholder.typicode.com";
    private static final Set<String> RESOURCES = Set.of(
            "posts", "comments", "albums", "photos", "todos", "users"
    );

    @Tool("获取模拟 REST API 数据，支持 posts/comments/albums/photos/todos/users。适用于生成 demo 项目的后端数据。")
    public String getMockData(@P("资源类型: posts|comments|albums|photos|todos|users") String resource,
                              @P(value = "获取单条数据的 ID，不填则返回列表") Integer id,
                              @P(value = "返回数量限制，不填则返回全部") Integer limit) {
        setupMonitorContext();

        if (resource == null || !RESOURCES.contains(resource.toLowerCase())) {
            return "不支持的资源类型，可选: " + String.join(", ", RESOURCES);
        }

        String url;
        if (id != null && id > 0) {
            url = API + "/" + resource + "/" + id;
        } else {
            url = API + "/" + resource;
            if (limit != null && limit > 0) {
                url += "?_limit=" + Math.min(limit, 100);
            }
        }

        String responseBody = doGetWithCache(url);
        if (responseBody == null) {
            return "获取模拟数据失败，请稍后重试";
        }

        return responseBody;
    }

    @Override
    public String getToolName() {
        return "mockApi";
    }

    @Override
    public String getDisplayName() {
        return "模拟数据";
    }

    @Override
    public String generateToolExecutedResult(cn.hutool.json.JSONObject arguments) {
        String resource = arguments.getStr("resource", "");
        return String.format("✅ %s → `%s`", getDisplayName(), resource);
    }
}
