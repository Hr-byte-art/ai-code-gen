package com.wjh.aicodegen.ai.tools;

import cn.hutool.json.JSONObject;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 网页内容获取工具
 * 支持 AI 通过工具调用的方式获取指定 URL 的内容
 *
 * @author 王哈哈
 */
@Slf4j
@Component
public class WebFetchTool extends BaseTool {

    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    @Tool("获取指定URL的网页内容。可用于读取技术文档、API参考页面、GitHub README等。返回网页的文本内容（自动去除HTML标签）。")
    public String webFetch(
            @P("要获取的URL地址") String url,
            @ToolMemoryId Long appId
    ) {
        try {
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://" + url;
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,text/plain")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                return "获取失败，状态码: " + response.statusCode();
            }

            String html = response.body();
            String text = extractText(html);

            // 截断过长内容
            if (text.length() > 8000) {
                text = text.substring(0, 8000) + "\n\n... (内容已截断，共 " + text.length() + " 字符)";
            }

            log.info("网页内容获取成功: url={}, length={}", url, text.length());
            return "网页内容 (" + url + "):\n\n" + text;

        } catch (Exception e) {
            log.error("网页内容获取失败: url={}, error={}", url, e.getMessage());
            return "获取失败: " + e.getMessage();
        }
    }

    private String extractText(String html) {
        // 移除 script 和 style 标签
        String text = html.replaceAll("<script[^>]*>[\\s\\S]*?</script>", "");
        text = text.replaceAll("<style[^>]*>[\\s\\S]*?</style>", "");
        // 移除 HTML 标签
        text = text.replaceAll("<[^>]+>", " ");
        // 清理空白
        text = text.replaceAll("&nbsp;", " ");
        text = text.replaceAll("&lt;", "<");
        text = text.replaceAll("&gt;", ">");
        text = text.replaceAll("&amp;", "&");
        text = text.replaceAll("&quot;", "\"");
        text = text.replaceAll("&#39;", "'");
        text = text.replaceAll("\\s+", " ");
        return text.trim();
    }

    @Override
    public String getToolName() {
        return "webFetch";
    }

    @Override
    public String getDisplayName() {
        return "获取网页内容";
    }

    @Override
    public String generateToolExecutedResult(JSONObject arguments) {
        String url = arguments.getStr("url");
        return String.format("✅ %s → `%s`", getDisplayName(), url);
    }
}
