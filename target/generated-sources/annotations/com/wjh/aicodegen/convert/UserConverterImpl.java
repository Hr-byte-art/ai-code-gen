package com.wjh.aicodegen.convert;

import com.wjh.aicodegen.model.dto.user.UserAddRequest;
import com.wjh.aicodegen.model.dto.user.UserUpdateRequest;
import com.wjh.aicodegen.model.entity.User;
import com.wjh.aicodegen.model.vo.user.LoginUserVO;
import com.wjh.aicodegen.model.vo.user.UserVO;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-28T23:23:16+0800",
    comments = "version: 1.6.3, compiler: javac, environment: Java 21.0.10 (Microsoft)"
)
@Component
public class UserConverterImpl implements UserConverter {

    @Override
    public User toUser(LoginUserVO loginUserVO) {
        if ( loginUserVO == null ) {
            return null;
        }

        User.UserBuilder user = User.builder();

        user.id( loginUserVO.getId() );
        user.userAccount( loginUserVO.getUserAccount() );
        user.userName( loginUserVO.getUserName() );
        user.userAvatar( loginUserVO.getUserAvatar() );
        user.userProfile( loginUserVO.getUserProfile() );
        user.userRole( loginUserVO.getUserRole() );
        user.createTime( loginUserVO.getCreateTime() );
        user.updateTime( loginUserVO.getUpdateTime() );
        user.vipExpireTime( loginUserVO.getVipExpireTime() );
        user.vipNumber( loginUserVO.getVipNumber() );
        user.shareCode( loginUserVO.getShareCode() );
        user.integral( loginUserVO.getIntegral() );
        user.recentlySignedIn( loginUserVO.getRecentlySignedIn() );

        return user.build();
    }

    @Override
    public LoginUserVO toLoginUserVO(User user) {
        if ( user == null ) {
            return null;
        }

        LoginUserVO loginUserVO = new LoginUserVO();

        loginUserVO.setId( user.getId() );
        loginUserVO.setUserAccount( user.getUserAccount() );
        loginUserVO.setUserName( user.getUserName() );
        loginUserVO.setUserAvatar( user.getUserAvatar() );
        loginUserVO.setUserProfile( user.getUserProfile() );
        loginUserVO.setUserRole( user.getUserRole() );
        loginUserVO.setCreateTime( user.getCreateTime() );
        loginUserVO.setUpdateTime( user.getUpdateTime() );
        loginUserVO.setVipExpireTime( user.getVipExpireTime() );
        loginUserVO.setVipNumber( user.getVipNumber() );
        loginUserVO.setShareCode( user.getShareCode() );
        loginUserVO.setIntegral( user.getIntegral() );
        loginUserVO.setRecentlySignedIn( user.getRecentlySignedIn() );

        return loginUserVO;
    }

    @Override
    public User toUser(UserUpdateRequest userUpdateRequest) {
        if ( userUpdateRequest == null ) {
            return null;
        }

        User.UserBuilder user = User.builder();

        user.id( userUpdateRequest.getId() );
        user.userName( userUpdateRequest.getUserName() );
        user.userAvatar( userUpdateRequest.getUserAvatar() );
        user.userProfile( userUpdateRequest.getUserProfile() );
        user.userRole( userUpdateRequest.getUserRole() );

        return user.build();
    }

    @Override
    public User toUser(UserAddRequest userAddRequest) {
        if ( userAddRequest == null ) {
            return null;
        }

        User.UserBuilder user = User.builder();

        user.userAccount( userAddRequest.getUserAccount() );
        user.userName( userAddRequest.getUserName() );
        user.userAvatar( userAddRequest.getUserAvatar() );
        user.userProfile( userAddRequest.getUserProfile() );
        user.userRole( userAddRequest.getUserRole() );

        return user.build();
    }

    @Override
    public UserVO toUserVO(User user) {
        if ( user == null ) {
            return null;
        }

        UserVO userVO = new UserVO();

        userVO.setId( user.getId() );
        userVO.setUserAccount( user.getUserAccount() );
        userVO.setUserName( user.getUserName() );
        userVO.setUserAvatar( user.getUserAvatar() );
        userVO.setUserProfile( user.getUserProfile() );
        userVO.setUserRole( user.getUserRole() );
        userVO.setCreateTime( user.getCreateTime() );
        userVO.setVipExpireTime( user.getVipExpireTime() );
        userVO.setVipCode( user.getVipCode() );
        userVO.setVipNumber( user.getVipNumber() );
        userVO.setShareCode( user.getShareCode() );
        userVO.setInviteUser( user.getInviteUser() );
        userVO.setIntegral( user.getIntegral() );

        return userVO;
    }
}
