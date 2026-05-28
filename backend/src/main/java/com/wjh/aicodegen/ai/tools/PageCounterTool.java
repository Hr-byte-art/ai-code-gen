package com.wjh.aicodegen.ai.tools;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 页面访问计数器工具
 * 使用 CountAPI（无需认证）
 */
@Slf4j
@Component
public class PageCounterTool extends PublicApiTool {

    private static final String API = "https://api.countapi.xyz";

    @Tool("创建页面访问计数器。返回计数器 ID 和前端调用代码，生成的项目可用 fetch 获取访问次数。")
    public String create(@P("命名空间，如 myapp") String namespace,
                         @P("计数器名称，如 homepage") String key) {
        setupMonitorContext();

        if (namespace == null || namespace.isBlank() || key == null || key.isBlank()) {
            return "请提供命名空间和计数器名称";
        }

        String ns = namespace.trim().replaceAll("[^a-zA-Z0-9_-]", "");
        String k = key.trim().replaceAll("[^a-zA-Z0-9_-]", "");

        String url = String.format("%s/hit/%s/%s", API, ns, k);
        String responseBody = doGet(url);

        if (responseBody == null) {
            return "创建计数器失败，请稍后重试";
        }

        try {
            JSONObject result = JSONUtil.parseObj(responseBody);
            Long value = result.getLong("value");
            return String.format("计数器已创建: %s/%s, 当前计数: %d\n\n前端调用示例:\nfetch('https://api.countapi.xyz/hit/%s/%s').then(r=>r.json()).then(d=>console.log(d.value))",
                    ns, k, value != null ? value : 0, ns, k);
        } catch (Exception e) {
            log.error("解析计数器响应失败: {}", e.getMessage());
            return "解析计数器数据失败";
        }
    }

    @Override
    public String getToolName() {
        return "pageCounter";
    }

    @Override
    public String getDisplayName() {
        return "访问计数";
    }

    @Override
    public String generateToolExecutedResult(cn.hutool.json.JSONObject arguments) {
        return String.format("✅ %s", getDisplayName());
    }
}
