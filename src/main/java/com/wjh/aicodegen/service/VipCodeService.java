package com.wjh.aicodegen.service;

import com.mybatisflex.core.service.IService;
import com.wjh.aicodegen.common.BaseResponse;
import com.wjh.aicodegen.model.dto.vipCode.VipCodeAddRequest;
import com.wjh.aicodegen.model.entity.VipCode;
import jakarta.servlet.http.HttpServletRequest;

/**
 *  服务层。
 *
 * @author 王哈哈
 * @since 2025-08-06 10:50:59
 */
public interface VipCodeService extends IService<VipCode> {

    /**
     * 添加数据
     *
     * @param vipCodeAddRequest
     * @return
     */
    Long addVipCode(VipCodeAddRequest vipCodeAddRequest , HttpServletRequest request);
}
