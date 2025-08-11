package com.wjh.aicodegen.service.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.wjh.aicodegen.convert.VipCodeConverter;
import com.wjh.aicodegen.exception.BusinessException;
import com.wjh.aicodegen.exception.ErrorCode;
import com.wjh.aicodegen.model.dto.vipCode.VipCodeAddRequest;
import com.wjh.aicodegen.model.entity.VipCode;
import com.wjh.aicodegen.mapper.VipCodeMapper;
import com.wjh.aicodegen.service.UserService;
import com.wjh.aicodegen.service.VipCodeService;
import com.wjh.aicodegen.utils.ResultUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 *  服务层实现。
 *
 * @author 王哈哈
 * @since 2025-08-06 10:50:59
 */
@Service
public class VipCodeServiceImpl extends ServiceImpl<VipCodeMapper, VipCode>  implements VipCodeService{

    @Resource
    private VipCodeConverter vipCodeConverter;

    @Resource
    @Lazy
    private UserService userService;

    @Override
    public Long addVipCode(VipCodeAddRequest vipCodeAddRequest , HttpServletRequest request) {
        // 设置默认值
        if (StrUtil.isBlank(vipCodeAddRequest.getVipCode())) {
            vipCodeAddRequest.setVipCode(RandomUtil.randomString(16));
        }
        if (vipCodeAddRequest.getEffectiveDay() == null) {
            vipCodeAddRequest.setEffectiveDay(365);
        }
        if (vipCodeAddRequest.getMaxUseNum() == null) {
            vipCodeAddRequest.setMaxUseNum(500);
        }
        if (vipCodeAddRequest.getExpDate() == null) {
            vipCodeAddRequest.setExpDate(LocalDateTime.now().plusDays(30));
        }
        String vipCode = vipCodeAddRequest.getVipCode();
        Integer effectiveDay = vipCodeAddRequest.getEffectiveDay();
        LocalDateTime expDate = vipCodeAddRequest.getExpDate();
        Integer maxUseNum = vipCodeAddRequest.getMaxUseNum();
        // 校验参数
        if (vipCode.length() < 16) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "会员码长度最低为16位");
        }

        if (effectiveDay < 1 || effectiveDay > 365) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "会员码有效期最低为1天，最高为365天");
        }

        if (maxUseNum < 1 || maxUseNum > 1000) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "会员码使用次数最低为1次，最高为1000次");
        }

        if (expDate.isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "会员码有效期不能早于当前时间");
        }

        // 检查是否已存在相同的会员码
        boolean exists = this.exists(new QueryWrapper().eq(VipCode::getVipCode, vipCode));
        if (exists) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "会员码已存在");
        }

        VipCode vipCodeEntity = vipCodeConverter.toEntity(vipCodeAddRequest);
        vipCodeEntity.setCreateUser(userService.getLoginUser(request).getId());
        this.save(vipCodeEntity);
        return vipCodeEntity.getId();
    }
}
