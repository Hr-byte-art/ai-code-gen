package com.wjh.aicodegen.core.parser;

import com.wjh.aicodegen.ai.model.MultiFileCodeResult;
import com.wjh.aicodegen.exception.BusinessException;
import com.wjh.aicodegen.exception.ErrorCode;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 多文件代码解析器（HTML + CSS + JS）
 *
 * @author 王哈哈
 */
public class MultiFileCodeParser implements CodeParser<MultiFileCodeResult> {

    private static final Pattern HTML_CODE_PATTERN = Pattern.compile("```html\\s*\\n([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);
    private static final Pattern CSS_CODE_PATTERN = Pattern.compile("```css\\s*\\n([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);
    private static final Pattern JS_CODE_PATTERN = Pattern.compile("```(?:js|javascript)\\s*\\n([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);

    @Override
    public MultiFileCodeResult parseCode(String codeContent) {
        String htmlCode = normalizeHtmlCode(extractRequiredCode(codeContent, HTML_CODE_PATTERN, "index.html"));
        String cssCode = extractRequiredCode(codeContent, CSS_CODE_PATTERN, "style.css").trim();
        String jsCode = extractRequiredCode(codeContent, JS_CODE_PATTERN, "script.js").trim();
        validateCompleteHtml(htmlCode);
        validateCss(cssCode);
        validateJs(jsCode);

        MultiFileCodeResult result = new MultiFileCodeResult();
        result.setHtmlCode(htmlCode);
        result.setCssCode(cssCode);
        result.setJsCode(jsCode);
        return result;
    }

    private String extractRequiredCode(String content, Pattern pattern, String fileName) {
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            String code = matcher.group(1);
            if (code != null && !code.trim().isEmpty()) {
                return code;
            }
        }
        throw new BusinessException(ErrorCode.SYSTEM_ERROR, fileName + " 代码块缺失或不完整，疑似模型输出被截断");
    }

    private String normalizeHtmlCode(String content) {
        String htmlCode = content == null ? "" : content.trim();
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
        String lowerCode = htmlCode == null ? "" : htmlCode.toLowerCase();
        boolean completeHtml = lowerCode.contains("<!doctype html")
                && lowerCode.contains("<html")
                && lowerCode.contains("</html>")
                && lowerCode.contains("<head")
                && lowerCode.contains("</head>")
                && lowerCode.contains("<body")
                && lowerCode.contains("</body>");
        if (!completeHtml) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "index.html 不完整，疑似模型输出被截断");
        }
        if (!lowerCode.contains("style.css") || !lowerCode.contains("script.js")) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "index.html 必须引用 style.css 和 script.js");
        }
    }

    private void validateCss(String cssCode) {
        if (cssCode == null || cssCode.isBlank()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "style.css 不能为空");
        }
        long leftBraces = cssCode.chars().filter(ch -> ch == '{').count();
        long rightBraces = cssCode.chars().filter(ch -> ch == '}').count();
        if (leftBraces != rightBraces) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "style.css 花括号不匹配，疑似模型输出被截断");
        }
    }

    private void validateJs(String jsCode) {
        if (jsCode == null || jsCode.isBlank()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "script.js 不能为空");
        }
        if (jsCode.contains("```")) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "script.js 包含 Markdown 代码块标记");
        }
    }
}
