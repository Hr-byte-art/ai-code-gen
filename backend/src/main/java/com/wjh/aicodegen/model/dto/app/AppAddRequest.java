package com.wjh.aicodegen.model.dto.app;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author 王哈哈
 */
@Data
public class AppAddRequest implements Serializable {

    /**
     * 应用初始化的 prompt
     */
    private String initPrompt;

    /**
     * 模板标识（可选）
     */
    private String templateKey;

    /**
     * 代码生成类型（可选，不指定则使用 auto 模式）
     */
    private String codeGenType;

    @Serial
    private static final long serialVersionUID = 1L;
}
