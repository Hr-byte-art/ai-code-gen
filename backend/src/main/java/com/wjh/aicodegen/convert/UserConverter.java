package com.wjh.aicodegen.convert;

import com.wjh.aicodegen.model.dto.user.UserAddRequest;
import com.wjh.aicodegen.model.dto.user.UserUpdateRequest;
import com.wjh.aicodegen.model.entity.User;
import com.wjh.aicodegen.model.vo.user.LoginUserVO;
import com.wjh.aicodegen.model.vo.user.UserVO;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/**
 * @Author 王哈哈
 * @Date 2025/8/5 21:25:47
 * @Description User 转换器
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserConverter {
    User toUser(LoginUserVO loginUserVO);
    LoginUserVO toLoginUserVO(User user);
    User toUser(UserUpdateRequest userUpdateRequest);
    User toUser(UserAddRequest userAddRequest);
    UserVO toUserVO(User user);
}
