package com.wjh.aicodegen.model.dto.vipCode;

import lombok.Data;

import java.io.Serializable;

/**
 * 会员码兑换请求。
 */
@Data
public class VipCodeRedemptionRequest implements Serializable {

    /**
     * 会员码
     */
    private String vipCode;
}