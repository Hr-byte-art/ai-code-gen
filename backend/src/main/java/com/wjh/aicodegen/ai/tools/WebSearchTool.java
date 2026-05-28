package com.wjh.aicodegen.ai.tools;

import cn.hutool.json.JSONObject;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 网页搜索工具
 * 使用 DuckDuckGo HTML 搜索（免费，无需 API key）
 * 参考 OpenClaw 的 DuckDuckGo provider 实现
 *
 * @author 王哈哈
 */
@Slf4j
@Component
public class WebSearchTool extends BaseTool {

    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private static final String DDG_ENDPOINT = "https://html.duckduckgo.com/html/";

    // 搜索结果缓存（5 分钟过期）
    private final Map<String, CachedResult> cache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 5 * 60 * 1000;

    private static final Pattern RESULT_LINK_PATTERN = Pattern.compile(
            "<a\\b[^>]*\\bclass=\"[^\"]*\\bresult__a\\b[^\"]*\"[^>]*href=\"([^\"]+)\"[^>]*>([\\s\\S]*?)</a>",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern SNIPPET_PATTERN = Pattern.compile(
            "<a\\b[^>]*\\bclass=\"[^\"]*\\bresult__snippet\\b[^\"]*\"[^>]*>([\\s\\S]*?)</a>",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern BOT_CHALLENGE_PATTERN = Pattern.compile(
            "g-recaptcha|are you a human|id=\"challenge-form\"",
            Pattern.CASE_INSENSITIVE
    );

    @Tool("搜索网页获取信息。可用于查找技术文档、API参考、代码示例、最佳实践等。返回搜索结果的标题、链接和摘要。")
    public String webSearch(
            @P("搜索关键词") String query,
            @ToolMemoryId Long appId
    ) {
        try {
            // 检查缓存
            String cacheKey = query.trim().toLowerCase();
            CachedResult cached = cache.get(cacheKey);
            if (cached != null && !cached.isExpired()) {
                log.info("命中搜索缓存: query={}", query);
                return cached.value;
            }

            String encodedQuery = java.net.URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url = DDG_ENDPOINT + "?q=" + encodedQuery;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent",
                            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                    .GET()
                    .timeout(Duration.ofSeconds(20))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                return "搜索失败，状态码: " + response.statusCode();
            }

            String html = response.body();

            // Bot 检测
            if (isBotChallenge(html)) {
                return "搜索被限制（触发了反爬虫验证），请稍后重试";
            }

            List<SearchResult> results = parseSearchResults(html);

            if (results.isEmpty()) {
                return "未找到与「" + query + "」相关的结果";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("搜索「").append(query).append("」的结果：\n\n");
            for (int i = 0; i < Math.min(results.size(), 5); i++) {
                SearchResult r = results.get(i);
                sb.append(String.format("- %s\n  链接: %s\n  摘要: %s\n\n", r.title, r.url, r.snippet));
            }

            String result = sb.toString().trim();

            // 写入缓存
            cache.put(cacheKey, new CachedResult(result));

            log.info("网页搜索完成: query={}, results={}", query, results.size());
            return result;

        } catch (Exception e) {
            log.error("网页搜索失败: query={}, error={}", query, e.getMessage());
            return "搜索失败: " + e.getMessage();
        }
    }

    private boolean isBotChallenge(String html) {
        return BOT_CHALLENGE_PATTERN.matcher(html).find()
                && !html.contains("result__a");
    }

    private List<SearchResult> parseSearchResults(String html) {
        List<SearchResult> results = new ArrayList<>();

        Matcher linkMatcher = RESULT_LINK_PATTERN.matcher(html);
        while (linkMatcher.find() && results.size() < 5) {
            String rawUrl = linkMatcher.group(1);
            String rawTitle = linkMatcher.group(2);

            String title = decodeHtmlEntities(stripHtml(rawTitle)).trim();
            String url = decodeDuckDuckGoUrl(rawUrl);

            // 找 snippet：从当前匹配位置之后搜索
            int matchEnd = linkMatcher.end();
            String remaining = html.substring(matchEnd);
            Matcher snippetMatcher = SNIPPET_PATTERN.matcher(remaining);
            String snippet = "";
            if (snippetMatcher.find()) {
                snippet = decodeHtmlEntities(stripHtml(snippetMatcher.group(1))).trim();
            }

            if (!title.isEmpty() && !url.isEmpty()) {
                results.add(new SearchResult(title, url, snippet));
            }
        }
        return results;
    }

    private String decodeDuckDuckGoUrl(String rawUrl) {
        try {
            String normalized = rawUrl.startsWith("//") ? "https:" + rawUrl : rawUrl;
            URI uri = URI.create(normalized);
            String uddg = uri.getQuery() != null ? getQueryParam(uri.getQuery(), "uddg") : null;
            if (uddg != null) {
                return URLDecoder.decode(uddg, StandardCharsets.UTF_8);
            }
        } catch (Exception ignored) {
        }
        return rawUrl;
    }

    private String getQueryParam(String query, String name) {
        for (String param : query.split("&")) {
            String[] kv = param.split("=", 2);
            if (kv.length == 2 && kv[0].equals(name)) {
                return kv[1];
            }
        }
        return null;
    }

    private String decodeHtmlEntities(String text) {
        String result = text
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&#x27;", "'")
                .replace("&#x2F;", "/")
                .replace("&nbsp;", " ");
        
        // 处理十进制 HTML 实体 &#123;
        result = Pattern.compile("&#(\\d+);").matcher(result)
                .replaceAll(m -> String.valueOf((char) Integer.parseInt(m.group(1))));
        
        // 处理十六进制 HTML 实体 &#x1F;
        result = Pattern.compile("&#x([0-9a-fA-F]+);").matcher(result)
                .replaceAll(m -> String.valueOf((char) Integer.parseInt(m.group(1), 16)));
        
        return result;
    }

    private String stripHtml(String html) {
        return html.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim();
    }

    private record SearchResult(String title, String url, String snippet) {}

    private static class CachedResult {
        final String value;
        final long expireAt;

        CachedResult(String value) {
            this.value = value;
            this.expireAt = System.currentTimeMillis() + CACHE_TTL_MS;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expireAt;
        }
    }

    @Override
    public String getToolName() {
        return "webSearch";
    }

    @Override
    public String getDisplayName() {
        return "网页搜索";
    }

    @Override
    public String generateToolExecutedResult(JSONObject arguments) {
        String query = arguments.getStr("query");
        return String.format("✅ %s → `%s`", getDisplayName(), query);
    }
}
