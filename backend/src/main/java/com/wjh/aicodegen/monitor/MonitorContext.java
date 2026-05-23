package com.wjh.aicodegen.monitor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author 木子宸
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonitorContext implements Serializable {

    private String userId;

    private String appId;

    /**
     * AI调用用途 - 表示这次AI调用的业务目的
     * 例如：ROUTING, CODE_GENERATION, IMAGE_SEARCH, CHAT_INTERACTION等
     * 替代原来的codeGenType，语义更清晰，便于Token消耗分析
     */
    private String aiCallPurpose;

    // ==== 兼容性方法：保持向后兼容 ====

    /**
     * @deprecated 使用 getAiCallPurpose() 替代
     *             为了向后兼容暂时保留
     */
    @Deprecated
    public String getCodeGenType() {
        return this.aiCallPurpose;
    }

    /**
     * @deprecated 使用 setAiCallPurpose() 替代
     *             为了向后兼容暂时保留
     */
    @Deprecated
    public void setCodeGenType(String codeGenType) {
        this.aiCallPurpose = codeGenType;
    }

    /**
     * 获取AI调用用途
     */
    public String getAiCallPurpose() {
        return this.aiCallPurpose;
    }

    /**
     * 设置AI调用用途
     */
    public void setAiCallPurpose(String aiCallPurpose) {
        this.aiCallPurpose = aiCallPurpose;
    }

    @Serial
    private static final long serialVersionUID = 1L;
}
