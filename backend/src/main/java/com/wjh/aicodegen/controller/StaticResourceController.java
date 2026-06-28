package com.wjh.aicodegen.controller;

import cn.hutool.core.util.StrUtil;
import com.wjh.aicodegen.constant.AppConstant;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.nio.file.Files;

/**
 * @author 王哈哈
 */
@RestController
@Tag(name = "静态资源接口")
public class StaticResourceController {

    // 应用部署根目录
    private static final String DEPLOY_ROOT_DIR = AppConstant.CODE_DEPLOY_ROOT_DIR;

    // 应用生成根目录（用于预览）
    private static final String OUTPUT_ROOT_DIR = AppConstant.CODE_OUTPUT_ROOT_DIR;

    /**
     * 部署后的静态资源访问
     * 访问格式：http://localhost:8123/api/code_deploy/{deployKey}[/{fileName}]
     */
    @GetMapping("/code_deploy/{deployKey}/**")
    @Operation(summary = "部署后的静态资源访问")
    public ResponseEntity<Resource> serveDeployResource(@PathVariable String deployKey, HttpServletRequest request) {
        return serveFile(DEPLOY_ROOT_DIR, deployKey, request);
    }

    /**
     * 兼容旧静态资源访问路径。
     */
    @GetMapping("/static/{deployKey}/**")
    @Operation(summary = "部署后的静态资源访问（兼容路径）")
    public ResponseEntity<Resource> serveLegacyDeployResource(@PathVariable String deployKey, HttpServletRequest request) {
        return serveFile(DEPLOY_ROOT_DIR, deployKey, request);
    }

    /**
     * 预览接口（无需部署，直接从 code_output 提供文件）
     * 访问格式：http://localhost:8123/api/static/preview/{appId}[/{fileName}]
     */
    @GetMapping("/static/preview/{appId}/**")
    @Operation(summary = "预览生成的代码（无需部署）")
    public ResponseEntity<Resource> servePreviewResource(@PathVariable String appId, HttpServletRequest request) {
        // 根据 appId 查找对应的目录
        String dirName = findOutputDir(appId);
        if (dirName == null) {
            return ResponseEntity.notFound().build();
        }
        return serveFile(OUTPUT_ROOT_DIR, dirName, extractResourcePathAfterPrefix(request, "/static/preview/" + appId));
    }

    /**
     * 查找 appId 对应的输出目录
     */
    private String findOutputDir(String appId) {
        File outputDir = new File(OUTPUT_ROOT_DIR);
        if (!outputDir.exists()) return null;
        String[] candidates = {"landing_page_" + appId, "html_" + appId, "multi_file_" + appId,
                "vue_project_" + appId, "react_ts_" + appId, "nextjs_" + appId, "fullstack_" + appId};
        for (String dir : candidates) {
            File candidateDir = new File(outputDir, dir);
            if (candidateDir.exists()) {
                // 对于 fullstack 类型，优先使用 frontend/dist 目录
                if (dir.startsWith("fullstack_")) {
                    File frontendDist = new File(candidateDir, "frontend/dist");
                    if (frontendDist.exists()) {
                        return dir + "/frontend/dist";
                    }
                }
                return dir;
            }
        }
        return null;
    }

    /**
     * 通用文件服务方法
     */
    private ResponseEntity<Resource> serveFile(String rootDir, String dirName, HttpServletRequest request) {
        return serveFile(rootDir, dirName, extractResourcePath(request, dirName));
    }

    /**
     * 通用文件服务方法
     */
    private ResponseEntity<Resource> serveFile(String rootDir, String dirName, String resourcePath) {
        try {
            if (StrUtil.isBlank(resourcePath) || "/".equals(resourcePath)) {
                resourcePath = "index.html";
            }
            while (resourcePath.startsWith("/")) {
                resourcePath = resourcePath.substring(1);
            }

            File baseDir = new File(rootDir, dirName);
            File file = new File(baseDir, resourcePath);
            String canonicalPath = file.getCanonicalPath();
            String canonicalBase = baseDir.getCanonicalPath();
            if (!canonicalPath.startsWith(canonicalBase + File.separator) && !canonicalPath.equals(canonicalBase)) {
                return ResponseEntity.badRequest().build();
            }
            if (!file.exists() || !file.isFile()) {
                return ResponseEntity.notFound().build();
            }
            Resource resource = new FileSystemResource(file);
            return ResponseEntity.ok()
                    .header("Content-Type", getContentTypeWithCharset(file.getAbsolutePath()))
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 从部署资源路径中提取文件相对路径。
     */
    private String extractResourcePath(HttpServletRequest request, String dirName) {
        String fullPath = request.getRequestURI();
        int dirIndex = fullPath.indexOf("/" + dirName + "/");
        if (dirIndex >= 0) {
            return fullPath.substring(dirIndex + dirName.length() + 1);
        }
        dirIndex = fullPath.indexOf("/" + dirName);
        if (dirIndex < 0) {
            return "";
        }
        return fullPath.substring(dirIndex + dirName.length() + 1);
    }

    /**
     * 从固定访问前缀后提取文件相对路径。
     */
    private String extractResourcePathAfterPrefix(HttpServletRequest request, String prefix) {
        String fullPath = request.getRequestURI();
        int prefixIndex = fullPath.indexOf(prefix);
        if (prefixIndex < 0) {
            return "";
        }
        return fullPath.substring(prefixIndex + prefix.length());
    }

    /**
     * 根据文件扩展名返回 Content-Type，文本类型自动加 charset
     */
    private String getContentTypeWithCharset(String filePath) {
        try {
            String contentType = Files.probeContentType(new File(filePath).toPath());
            if (contentType != null) {
                // 文本类型自动加 charset
                if (contentType.startsWith("text/") || contentType.contains("javascript") || contentType.contains("json") || contentType.contains("xml")) {
                    return contentType + "; charset=UTF-8";
                }
                return contentType;
            }
        } catch (Exception ignored) {
        }
        return "application/octet-stream";
    }
}
