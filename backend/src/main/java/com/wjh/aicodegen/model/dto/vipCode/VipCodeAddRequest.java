package com.wjh.aicodegen.model.dto.vipCode;

import com.mybatisflex.annotation.Column;
import com.wjh.aicodegen.model.entity.VipCode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @Author 王哈哈
 * @Date 2025/8/11 11:30:05
 * @Description 添加请求
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class VipCodeAddRequest {

    /**
     * vipCode
     */
    private String vipCode;

    /**
     * vipCode的兑换的VIP的有效时间（天）
     */
    private Integer effectiveDay;

    /**
     * 该code的过期时间
     */
    private LocalDateTime expDate;

    /**
     * 最大使用人数
     */
    private Integer maxUseNum;

    /**
     * 创建人
     */
    private Long creatorId;
}
