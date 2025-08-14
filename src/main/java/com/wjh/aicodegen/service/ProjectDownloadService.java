package com.wjh.aicodegen.service;

import jakarta.servlet.http.HttpServletResponse;

/**
 * @Author 王哈哈
 * @Date 2025/8/14 14:04:30
 * @Description
 */
public interface ProjectDownloadService {
    /**
     * 下载项目
     * @param projectPath 项目路径
     * @param downloadFileName 下载文件名
     * @param response 响应
     */
    void downloadProjectAsZip(String projectPath, String downloadFileName, HttpServletResponse response);
}
