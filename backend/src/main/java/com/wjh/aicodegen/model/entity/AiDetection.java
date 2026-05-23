package com.wjh.aicodegen.model.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @Author 王哈哈
 * @Date 2025/8/24 19:10:10
 * @Description 检测结果
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AiDetection {
    /**
     * 是否安全
     */
    private boolean isSafe;

    /**
     * 违规词语
     */
    private List<String> reason;
}
