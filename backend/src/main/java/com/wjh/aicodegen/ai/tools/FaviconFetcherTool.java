package com.wjh.aicodegen.ai.tools;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 网站 Favicon 获取工具
 * 使用 iconhorse.dev（无需认证）
 */
@Slf4j
@Component
public class FaviconFetcherTool extends PublicApiTool {

    @Tool("获取任意网站的 favicon 图标 URL。输入域名，返回图标链接。适用于书签、导航类应用。")
    public String fetch(@P("网站域名，如 google.com") String domain) {
        setupMonitorContext();

        if (domain == null || domain.isBlank()) {
            return "请输入域名";
        }

        String cleanDomain = domain.trim()
                .replace("https://", "")
                .replace("http://", "")
                .replace("www.", "")
                .split("/")[0];

        String url = "https://favicon.iconhorse.dev/" + cleanDomain;
        return "Favicon URL: " + url;
    }

    @Override
    public String getToolName() {
        return "faviconFetcher";
    }

    @Override
    public String getDisplayName() {
        return "网站图标";
    }

    @Override
    public String generateToolExecutedResult(cn.hutool.json.JSONObject arguments) {
        String domain = arguments.getStr("domain", "");
        return String.format("✅ %s → `%s`", getDisplayName(), domain);
    }
}
