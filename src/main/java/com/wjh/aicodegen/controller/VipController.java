package com.wjh.aicodegen.controller;

import com.wjh.aicodegen.common.BaseResponse;
import com.wjh.aicodegen.model.vo.user.UserVO;
import com.wjh.aicodegen.service.UserService;
import com.wjh.aicodegen.utils.ResultUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
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
    public BaseResponse<UserVO> vipCodeRedemption(String vipCode , HttpServletRequest request){
        UserVO userVO = userService.vipCodeRedemption(vipCode, request);
        return ResultUtils.success(userVO);
    }

}
