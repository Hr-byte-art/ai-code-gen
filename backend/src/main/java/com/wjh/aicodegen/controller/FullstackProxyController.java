package com.wjh.aicodegen.controller;

import com.wjh.aicodegen.core.deploy.NodeProcessManager;
import com.wjh.aicodegen.exception.BusinessException;
import com.wjh.aicodegen.exception.ErrorCode;
import com.wjh.aicodegen.utils.ThrowUtils;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

/**
 * 全栈应用公网代理入口
 */
@Hidden
@RestController
@RequestMapping("/fullstack")
public class FullstackProxyController {

    private static final Set<String> HOP_BY_HOP_HEADERS = Set.of(
            "connection", "keep-alive", "proxy-authenticate", "proxy-authorization",
            "te", "trailers", "transfer-encoding", "upgrade", "host", "content-length"
    );

    @Resource
    private NodeProcessManager nodeProcessManager;

    @RequestMapping({"/{appId}", "/{appId}/**"})
    public ResponseEntity<byte[]> proxy(HttpServletRequest request) {
        Long appId = extractAppId(request);
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        ThrowUtils.throwIf(!nodeProcessManager.isReady(appId), ErrorCode.NOT_FOUND_ERROR, "全栈服务未运行，请先部署应用");

        String targetUrl = buildTargetUrl(appId, request);
        try {
            HttpURLConnection connection = (HttpURLConnection) URI.create(targetUrl).toURL().openConnection();
            connection.setRequestMethod(request.getMethod());
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(30000);
            copyRequestHeaders(request, connection);

            byte[] requestBody = StreamUtils.copyToByteArray(request.getInputStream());
            if (requestBody.length > 0 && allowsBody(request.getMethod())) {
                connection.setDoOutput(true);
                try (OutputStream outputStream = connection.getOutputStream()) {
                    outputStream.write(requestBody);
                }
            }

            int status = connection.getResponseCode();
            byte[] responseBody = readResponseBody(connection, status);
            HttpHeaders responseHeaders = copyResponseHeaders(connection);
            responseBody = rewriteHtmlBaseIfNeeded(responseBody, responseHeaders, appId);
            return new ResponseEntity<>(responseBody, responseHeaders, HttpStatus.valueOf(status));
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "全栈服务代理失败：" + e.getMessage());
        }
    }

    private Long extractAppId(HttpServletRequest request) {
        String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        if (path == null) return null;
        String[] parts = path.split("/");
        for (int i = 0; i < parts.length; i++) {
            if ("fullstack".equals(parts[i]) && i + 1 < parts.length) {
                try {
                    return Long.valueOf(parts[i + 1]);
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    private String buildTargetUrl(Long appId, HttpServletRequest request) {
        String baseUrl = nodeProcessManager.getUrl(appId);
        ThrowUtils.throwIf(baseUrl == null, ErrorCode.SYSTEM_ERROR, "全栈服务地址不存在");

        String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        String prefix = "/fullstack/" + appId;
        String upstreamPath = "/";
        if (path != null) {
            int index = path.indexOf(prefix);
            if (index >= 0) {
                upstreamPath = path.substring(index + prefix.length());
                if (upstreamPath.isBlank()) upstreamPath = "/";
            }
        }
        String query = request.getQueryString();
        return baseUrl + upstreamPath + (query == null ? "" : "?" + query);
    }

    private void copyRequestHeaders(HttpServletRequest request, HttpURLConnection connection) {
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            if (HOP_BY_HOP_HEADERS.contains(name.toLowerCase())) continue;
            Enumeration<String> values = request.getHeaders(name);
            while (values.hasMoreElements()) {
                connection.addRequestProperty(name, values.nextElement());
            }
        }
    }

    private HttpHeaders copyResponseHeaders(HttpURLConnection connection) {
        HttpHeaders headers = new HttpHeaders();
        Set<String> copied = new HashSet<>();
        connection.getHeaderFields().forEach((name, values) -> {
            if (name == null || HOP_BY_HOP_HEADERS.contains(name.toLowerCase())) return;
            if (!copied.add(name.toLowerCase())) return;
            for (String value : values == null ? Collections.<String>emptyList() : values) {
                headers.add(name, value);
            }
        });
        return headers;
    }

    private byte[] readResponseBody(HttpURLConnection connection, int status) throws Exception {
        InputStream inputStream = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
        if (inputStream == null) return new byte[0];
        try (inputStream) {
            return StreamUtils.copyToByteArray(inputStream);
        }
    }

    private byte[] rewriteHtmlBaseIfNeeded(byte[] body, HttpHeaders headers, Long appId) {
        String contentType = headers.getFirst(HttpHeaders.CONTENT_TYPE);
        if (contentType == null || !contentType.toLowerCase().contains("text/html")) return body;
        String html = new String(body, StandardCharsets.UTF_8);
        String base = "<base href=\"/api/fullstack/" + appId + "/\">";
        String bridgeScript = buildApiBridgeScript(appId);
        if (html.contains("<base ")) return body;
        if (html.contains("<head>")) {
            html = html.replace("<head>", "<head>\n  " + base + "\n  " + bridgeScript);
        }
        headers.remove(HttpHeaders.CONTENT_LENGTH);
        return html.getBytes(StandardCharsets.UTF_8);
    }

    private String buildApiBridgeScript(Long appId) {
        String prefix = "/api/fullstack/" + appId;
        return "<script>(function(){" +
                "var p='" + prefix + "';" +
                "function r(u){if(typeof u!=='string')return u;var o=location.origin;if(u.indexOf(p+'/api/')===0||u.indexOf(o+p+'/api/')===0)return u;if(u.indexOf('/api/')===0)return p+u.slice(4);if(u.indexOf(o+'/api/')===0)return o+p+u.slice(o.length+4);return u}" +
                "var f=window.fetch;" +
                "if(f){window.fetch=function(u,o){if(u instanceof Request){var n=r(u.url);if(n!==u.url)u=new Request(n,u)}else{u=r(u)}return f.call(this,u,o)}}" +
                "var x=window.XMLHttpRequest&&window.XMLHttpRequest.prototype.open;" +
                "if(x){window.XMLHttpRequest.prototype.open=function(m,u){arguments[1]=r(u);return x.apply(this,arguments)}}" +
                "})();</script>";
    }

    private boolean allowsBody(String method) {
        return HttpMethod.POST.matches(method) || HttpMethod.PUT.matches(method)
                || HttpMethod.PATCH.matches(method) || HttpMethod.DELETE.matches(method);
    }
}