package com.wjh.aicodegen.service;

/**
 * @Author 王哈哈
 * @Date 2025/8/14 10:15:29
 * @Description 截图服务
 */
public interface ScreenshotService {
    /**
     * 生成并上传截图
     *
     * @param webUrl 网页地址
     * @return 截图URL
     */
    String generateAndUploadScreenshot(String webUrl);
}
