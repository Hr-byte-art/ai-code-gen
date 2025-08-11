package com.wjh.aicodegen.convert;

import com.wjh.aicodegen.model.dto.vipCode.VipCodeAddRequest;
import com.wjh.aicodegen.model.entity.VipCode;
import org.mapstruct.Mapper;

/**
 * @Author 王哈哈
 * @Date 2025/8/11 11:32:45
 * @Description 会员码转换器
 */
@Mapper(componentModel = "spring")
public interface VipCodeConverter {

    VipCode toEntity(VipCodeAddRequest vipCodeAddRequest);
}
