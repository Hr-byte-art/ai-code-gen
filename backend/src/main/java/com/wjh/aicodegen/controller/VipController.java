package com.wjh.aicodegen.controller;

import cn.hutool.core.util.StrUtil;
import com.wjh.aicodegen.common.BaseResponse;
import com.wjh.aicodegen.exception.ErrorCode;
import com.wjh.aicodegen.model.dto.vipCode.VipCodeRedemptionRequest;
import com.wjh.aicodegen.model.vo.user.UserVO;
import com.wjh.aicodegen.service.UserService;
import com.wjh.aicodegen.utils.ResultUtils;
import com.wjh.aicodegen.utils.ThrowUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author 王哈哈
 * @Date 2025/8/6 11:34:12
 * @Description 会员相关接口
 */
@RestController
@Tag(name = "会员相关接口")
public class VipController {

    @Resource
    private UserService userService;

    @PostMapping("/vip/vipCodeRedemption")
    @Operation(summary =  "会员码兑换" , responses = {@ApiResponse(description = "会员信息")})
    public BaseResponse<UserVO> vipCodeRedemption(@RequestBody VipCodeRedemptionRequest redemptionRequest,
                                                  HttpServletRequest request){
        ThrowUtils.throwIf(redemptionRequest == null || StrUtil.isBlank(redemptionRequest.getVipCode()),
                ErrorCode.PARAMS_ERROR, "会员码不能为空");
        UserVO userVO = userService.vipCodeRedemption(redemptionRequest.getVipCode().trim(), request);
        return ResultUtils.success(userVO);
    }

}
