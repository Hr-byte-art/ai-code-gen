package com.wjh.aicodegen.core.parser;

import com.wjh.aicodegen.ai.model.HtmlCodeResult;
import com.wjh.aicodegen.exception.BusinessException;
import com.wjh.aicodegen.exception.ErrorCode;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * HTML 单文件代码解析器
 *
 * @author 王哈哈
 */
public class HtmlCodeParser implements CodeParser<HtmlCodeResult> {

    private static final Pattern HTML_CODE_PATTERN = Pattern.compile("```html\\s*\\n([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);

    @Override
    public HtmlCodeResult parseCode(String codeContent) {
        String htmlCode = extractHtmlCode(codeContent);
        String normalizedCode = normalizeHtmlCode(htmlCode != null ? htmlCode : codeContent);
        validateCompleteHtml(normalizedCode);

        HtmlCodeResult result = new HtmlCodeResult();
        result.setHtmlCode(normalizedCode);
        return result;
    }

    /**
     * 提取HTML代码内容
     *
     * @param content 原始内容
     * @return HTML代码
     */
    private String extractHtmlCode(String content) {
        Matcher matcher = HTML_CODE_PATTERN.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private String normalizeHtmlCode(String content) {
        if (content == null) {
            return "";
        }
        String htmlCode = content.trim();
        if (htmlCode.startsWith("```html")) {
            htmlCode = htmlCode.substring("```html".length()).trim();
        } else if (htmlCode.startsWith("```")) {
            htmlCode = htmlCode.substring("```".length()).trim();
        }
        if (htmlCode.endsWith("```")) {
            htmlCode = htmlCode.substring(0, htmlCode.length() - "```".length()).trim();
        }
        return extractHtmlDocument(htmlCode);
    }

    private String extractHtmlDocument(String htmlCode) {
        String lowerCode = htmlCode.toLowerCase();
        int start = lowerCode.indexOf("<!doctype html");
        if (start < 0) {
            start = lowerCode.indexOf("<html");
        }
        int end = lowerCode.lastIndexOf("</html>");
        if (start >= 0 && end >= start) {
            return htmlCode.substring(start, end + "</html>".length()).trim();
        }
        return htmlCode;
    }

    private void validateCompleteHtml(String htmlCode) {
        if (htmlCode == null || htmlCode.isBlank()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "HTML 代码为空");
        }
        String lowerCode = htmlCode.toLowerCase();
        boolean completeHtml = lowerCode.contains("<!doctype html")
                && lowerCode.contains("<html")
                && lowerCode.contains("</html>")
                && lowerCode.contains("<head")
                && lowerCode.contains("</head>")
                && lowerCode.contains("<body")
                && lowerCode.contains("</body>");
        if (!completeHtml) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "HTML 代码不完整，疑似模型输出被截断");
        }
    }
}
