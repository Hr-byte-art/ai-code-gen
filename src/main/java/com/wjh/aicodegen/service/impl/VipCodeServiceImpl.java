package com.wjh.aicodegen.service.impl;

import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.wjh.aicodegen.model.entity.VipCode;
import com.wjh.aicodegen.mapper.VipCodeMapper;
import com.wjh.aicodegen.service.VipCodeService;
import org.springframework.stereotype.Service;

/**
 *  服务层实现。
 *
 * @author 王哈哈
 * @since 2025-08-06 10:50:59
 */
@Service
public class VipCodeServiceImpl extends ServiceImpl<VipCodeMapper, VipCode>  implements VipCodeService{

}
