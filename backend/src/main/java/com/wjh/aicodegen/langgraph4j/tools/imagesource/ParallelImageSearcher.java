package com.wjh.aicodegen.langgraph4j.tools.imagesource;

import com.wjh.aicodegen.langgraph4j.model.ImageResource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 多源并行图片搜索器
 * 自动发现所有 ImageSource 实现，并行调用，合并去重排序
 */
@Slf4j
@Component
public class ParallelImageSearcher {

    private final List<ImageSource> imageSources;

    public ParallelImageSearcher(List<ImageSource> imageSources) {
        this.imageSources = imageSources;
        log.info("图片搜索源初始化完成，共 {} 个源: {}",
                imageSources.size(),
                imageSources.stream().map(ImageSource::getName).collect(Collectors.joining(", ")));
    }

    /**
     * 并行搜索所有图片源，合并去重排序后返回 Top N
     *
     * @param query 搜索关键词
     * @param count 期望返回数量
     * @return 合并去重排序后的图片列表
     */
    public List<ImageResource> search(String query, int count) {
        if (imageSources.isEmpty()) {
            log.warn("没有可用的图片搜索源");
            return List.of();
        }

        int perSource = Math.max(count / imageSources.size() + 1, 3);

        // 并行请求所有源
        List<CompletableFuture<List<ImageResource>>> futures = imageSources.stream()
                .map(source -> CompletableFuture
                        .supplyAsync(() -> {
                            try {
                                long start = System.currentTimeMillis();
                                List<ImageResource> results = source.search(query, perSource);
                                long elapsed = System.currentTimeMillis() - start;
                                log.info("图片源 [{}] 搜索完成: keyword='{}', found={}, elapsed={}ms",
                                        source.getName(), query, results.size(), elapsed);
                                return results;
                            } catch (Exception e) {
                                log.error("图片源 [{}] 搜索异常: {}", source.getName(), e.getMessage());
                                return List.<ImageResource>of();
                            }
                        })
                        .completeOnTimeout(List.of(), 5, TimeUnit.SECONDS)
                        .exceptionally(ex -> {
                            log.error("图片源 [{}] 搜索超时或异常: {}", source.getName(), ex.getMessage());
                            return List.of();
                        }))
                .toList();

        // 等待所有完成，合并结果
        List<ImageResource> allImages = new ArrayList<>();
        for (CompletableFuture<List<ImageResource>> future : futures) {
            try {
                allImages.addAll(future.join());
            } catch (Exception e) {
                log.error("等待图片搜索结果异常: {}", e.getMessage());
            }
        }

        // 按 URL 去重，保留先出现的
        List<ImageResource> deduped = allImages.stream()
                .filter(img -> img.getUrl() != null && !img.getUrl().isBlank())
                .collect(Collectors.toMap(
                        ImageResource::getUrl,
                        img -> img,
                        (a, b) -> a,
                        java.util.LinkedHashMap::new
                ))
                .values()
                .stream()
                .collect(Collectors.toList());

        // 打分排序
        deduped.sort(Comparator.comparingInt(this::scoreImage).reversed());

        // 取 Top N
        List<ImageResource> result = deduped.stream()
                .limit(count)
                .collect(Collectors.toList());

        log.info("图片搜索汇总: keyword='{}', 总候选={}, 去重后={}, 返回={}",
                query, allImages.size(), deduped.size(), result.size());

        return result;
    }

    /**
     * 图片质量评分
     * Pexels 来源略加分（质量更高），URL 包含 medium/large/original 加分
     */
    private int scoreImage(ImageResource img) {
        int score = 0;
        String url = img.getUrl();

        // Pexels 来源加分（URL 包含 pexels.com）
        if (url.contains("pexels.com")) {
            score += 10;
        }

        // 高分辨率关键词加分
        if (url.contains("large") || url.contains("original")) {
            score += 5;
        } else if (url.contains("medium")) {
            score += 3;
        }

        // 有描述加分
        if (img.getDescription() != null && !img.getDescription().isBlank()
                && !img.getDescription().equals(img.getUrl())) {
            score += 2;
        }

        return score;
    }
}
