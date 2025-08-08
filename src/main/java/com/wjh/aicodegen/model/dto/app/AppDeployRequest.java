package com.wjh.aicodegen.model.dto.app;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author 木子宸
 */
@Data
public class AppDeployRequest implements Serializable {

    /**
     * 应用 id
     */
    private Long appId;

    @Serial
    private static final long serialVersionUID = 1L;
}
