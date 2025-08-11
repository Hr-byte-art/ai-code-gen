package com.wjh.aicodegen.model.dto.vipCode;

import com.mybatisflex.annotation.Column;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @Author 王哈哈
 * @Date 2025/8/11 13:47:11
 * @Description 修改会员码
 */
@Data
public class VipCodeUpdateRequest {
    /**
     * id
     */
    private Long id;
    /**
     * 该code的过期时间
     */
    private LocalDateTime expDate;

    /**
     * 最大使用人数
     */
    private Integer maxUseNum;
}
