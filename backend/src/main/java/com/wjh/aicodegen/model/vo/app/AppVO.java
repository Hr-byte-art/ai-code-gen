package com.wjh.aicodegen.model.vo.app;

import com.wjh.aicodegen.model.vo.user.UserVO;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class AppVO implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 应用名称
     */
    private String appName;

    /**
     * 应用封面
     */
    private String cover;

    /**
     * 应用初始化的 prompt
     */
    private String initPrompt;

    /**
     * 代码生成类型（枚举）
     */
    private String codeGenType;

    /**
     * 设计风格标识
     */
    private String designKey;

    /**
     * 部署标识
     */
    private String deployKey;

    /**
     * 部署时间
     */
    private LocalDateTime deployedTime;

    /**
     * 构建状态：none / pending / building / success / failed
     */
    private String buildStatus;

    /**
     * 构建状态说明
     */
    private String buildMessage;

    /**
     * 构建错误摘要
     */
    private String buildError;

    /**
     * 构建重试次数
     */
    private Integer buildRetryCount;

    /**
     * 构建开始时间
     */
    private LocalDateTime buildStartedTime;

    /**
     * 构建完成时间
     */
    private LocalDateTime buildFinishedTime;

    /**
     * 优先级
     */
    private Integer priority;

    /**
     * 创建用户id
     */
    private Long userId;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 创建用户信息
     */
    private UserVO user;

    private static final long serialVersionUID = 1L;
}
