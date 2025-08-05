package com.wjh.aicodegen.convert;

import com.wjh.aicodegen.mdoel.dto.user.UserAddRequest;
import com.wjh.aicodegen.mdoel.dto.user.UserUpdateRequest;
import com.wjh.aicodegen.mdoel.entity.User;
import com.wjh.aicodegen.mdoel.vo.user.LoginUserVO;
import org.mapstruct.Mapper;

/**
 * @Author 王哈哈
 * @Date 2025/8/5 21:25:47
 * @Description User 转换器
 */
@Mapper(componentModel = "spring")
public interface UserConverter {
    User toUser(LoginUserVO loginUserVO);
    LoginUserVO toLoginUserVO(User user);
    User toUser(UserUpdateRequest userUpdateRequest);
    User toUser(UserAddRequest userAddRequest);
}
