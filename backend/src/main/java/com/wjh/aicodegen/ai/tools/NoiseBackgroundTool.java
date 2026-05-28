package com.wjh.aicodegen.ai.tools;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 噪点背景图生成工具
 * 使用 php-noise API（无需认证）
 */
@Slf4j
@Component
public class NoiseBackgroundTool extends PublicApiTool {

    private static final String API = "https://php-noise.com/noise.php";

    @Tool("生成噪点纹理背景图片 URL。可指定尺寸和颜色（HEX），用于设计感页面。")
    public String generate(@P(value = "宽度，默认 800") Integer width,
                           @P(value = "高度，默认 600") Integer height,
                           @P(value = "HEX 颜色，如 ff6600，不填随机") String color) {
        setupMonitorContext();

        int w = (width != null && width > 0) ? Math.min(width, 2000) : 800;
        int h = (height != null && height > 0) ? Math.min(height, 2000) : 600;

        StringBuilder url = new StringBuilder(API);
        url.append("?w=").append(w).append("&h=").append(h).append("&tile&json");

        if (color != null && !color.isBlank()) {
            String cleanColor = color.trim().replace("#", "");
            url.append("&color=").append(cleanColor);
        }

        return "噪点背景图片 URL: " + url.toString().replace("&json", "");
    }

    @Override
    public String getToolName() {
        return "noiseBackground";
    }

    @Override
    public String getDisplayName() {
        return "噪点背景";
    }

    @Override
    public String generateToolExecutedResult(cn.hutool.json.JSONObject arguments) {
        return String.format("✅ %s", getDisplayName());
    }
}
