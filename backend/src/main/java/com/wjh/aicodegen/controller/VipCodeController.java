package com.wjh.aicodegen.controller;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.wjh.aicodegen.annotation.AuthCheck;
import com.wjh.aicodegen.common.BaseResponse;
import com.wjh.aicodegen.common.DeleteRequest;
import com.wjh.aicodegen.constant.UserConstant;
import com.wjh.aicodegen.convert.VipCodeConverter;
import com.wjh.aicodegen.exception.BusinessException;
import com.wjh.aicodegen.exception.ErrorCode;
import com.wjh.aicodegen.model.dto.vipCode.VipCodeAddRequest;
import com.wjh.aicodegen.model.dto.vipCode.VipCodeUpdateRequest;
import com.wjh.aicodegen.model.entity.VipCode;
import com.wjh.aicodegen.model.enums.UserRoleEnum;
import com.wjh.aicodegen.service.VipCodeService;
import com.wjh.aicodegen.utils.ResultUtils;
import com.wjh.aicodegen.utils.ThrowUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

/**
 *  控制层。
 *
 * @author 王哈哈
 * @since 2025-08-06 10:50:59
 */
@RestController
@RequestMapping("/vipCode")
@Tag(name = "会员码接口")
public class VipCodeController {

    @Resource
    private VipCodeService vipCodeService;

    @Resource
    private VipCodeConverter vipCodeConverter;

    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @Operation(summary = "添加会员码", responses = {@ApiResponse(description = "会员码id")})
    public BaseResponse<Long> addVipCode(@RequestBody VipCodeAddRequest vipCodeAddRequest , HttpServletRequest request) {
        Long addVipCodeId = vipCodeService.addVipCode(vipCodeAddRequest , request);
        return ResultUtils.success(addVipCodeId);
    }



    /**
     * 更新会员码
     * @param vipCodeUpdateRequest 更新会员码参数
     * @return BaseResponse<Long>
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @Operation(summary = "更新会员码", responses = {@ApiResponse(description = "会员码id")
    })
    public BaseResponse<Long> updateVipCode(@RequestBody VipCodeUpdateRequest vipCodeUpdateRequest) {
        VipCode vipCode = vipCodeService.getById(vipCodeUpdateRequest.getId());
        ThrowUtils.throwIf(vipCode == null , ErrorCode.NOT_FOUND_ERROR , "会员码不存在");
        ThrowUtils.throwIf(vipCodeUpdateRequest.getMaxUseNum() < vipCode.getUseNum() , ErrorCode.OPERATION_ERROR , "会员码使用次数不能小于已使用的次数");
        if (vipCodeUpdateRequest.getExpDate() != null && vipCodeUpdateRequest.getExpDate().isAfter(LocalDateTime.now())){
            vipCode.setExpDate(vipCodeUpdateRequest.getExpDate());
        }
        if (vipCodeUpdateRequest.getMaxUseNum() != null && vipCodeUpdateRequest.getMaxUseNum() > 0){
            vipCode.setMaxUseNum(vipCodeUpdateRequest.getMaxUseNum());
        }
        vipCodeService.updateById(vipCode);
        return ResultUtils.success(vipCode.getId());
    }

    /**
     * 获取会员码
     * @param id 会员码id
     * @return BaseResponse<VipCode>
     */
    @GetMapping("/get")
    @Operation(summary = "获取会员码详细信息")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<VipCode> getVipCodeById(long id) {
        return ResultUtils.success(vipCodeService.getById(id));
    }

    /**
     * 获取会员码列表
     * @return BaseResponse<List<VipCode>>
     */
    @GetMapping("/list")
    @Operation(summary = "获取会员码列表")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<List<VipCode>> listVipCode() {
        return ResultUtils.success(vipCodeService.list());
    }

    /**
     * 删除会员码
     * @param deleteRequest 会员码id
     * @return BaseResponse<Boolean>
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @Operation(summary = "删除会员码")
    public BaseResponse<Boolean> deleteVipCode(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        VipCode vipCode = vipCodeService.getById(deleteRequest.getId());
        ThrowUtils.throwIf(vipCode == null , ErrorCode.NOT_FOUND_ERROR , "会员码不存在");
        ThrowUtils.throwIf(vipCode.getUseNum() > 0 , ErrorCode.OPERATION_ERROR , "会员码已使用，不能删除");
        return ResultUtils.success(vipCodeService.removeById(deleteRequest.getId()));
    }
}
