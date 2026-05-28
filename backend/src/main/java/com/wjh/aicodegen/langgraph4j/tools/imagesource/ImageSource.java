package com.wjh.aicodegen.langgraph4j.tools.imagesource;

import com.wjh.aicodegen.langgraph4j.model.ImageResource;

import java.util.List;

/**
 * 图片搜索源接口
 * 实现此接口即可被 ParallelImageSearcher 自动发现和并行调用
 */
public interface ImageSource {

    /**
     * 图片源名称（用于日志和去重标记）
     */
    String getName();

    /**
     * 搜索图片
     *
     * @param query 搜索关键词
     * @param count 期望返回数量
     * @return 图片资源列表，异常或无结果时返回空列表
     */
    List<ImageResource> search(String query, int count);
}
