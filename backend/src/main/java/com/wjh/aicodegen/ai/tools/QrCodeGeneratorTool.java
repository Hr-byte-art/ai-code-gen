package com.wjh.aicodegen.ai.tools;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 二维码生成工具
 * 使用 goqr.me API（无需认证）
 */
@Slf4j
@Component
public class QrCodeGeneratorTool extends PublicApiTool {

    private static final String API = "https://api.qrserver.com/v1/create-qr-code/";

    @Tool("生成二维码图片 URL。输入文本或链接，返回二维码图片地址，可直接用于 <img src>。")
    public String generate(@P("要编码的文本或 URL") String text,
                           @P(description = "尺寸(像素)",required = false ,defaultValue = "200") Integer size) {
        setupMonitorContext();

        if (text == null || text.isBlank()) {
            return "请输入要编码的文本";
        }

        int qrSize = (size != null && size > 0) ? Math.min(size, 1000) : 200;
        String encoded = URLEncoder.encode(text.trim(), StandardCharsets.UTF_8);
        String url = String.format("%s?size=%dx%d&data=%s", API, qrSize, qrSize, encoded);

        return "二维码图片 URL: " + url;
    }

    @Override
    public String getToolName() {
        return "qrCodeGenerator";
    }

    @Override
    public String getDisplayName() {
        return "二维码生成";
    }

    @Override
    public String generateToolExecutedResult(cn.hutool.json.JSONObject arguments) {
        return String.format("%s", getDisplayName());
    }
}
