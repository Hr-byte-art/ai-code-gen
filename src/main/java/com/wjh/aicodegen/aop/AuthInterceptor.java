package com.wjh.aicodegen.aop;

import com.wjh.aicodegen.annotation.AuthCheck;
import com.wjh.aicodegen.exception.BusinessException;
import com.wjh.aicodegen.exception.ErrorCode;
import com.wjh.aicodegen.mdoel.entity.User;
import com.wjh.aicodegen.mdoel.enums.UserRoleEnum;
import com.wjh.aicodegen.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * @Author 王哈哈
 * @Date 2025/8/5 22:05:21
 * @Description
 */
@Aspect
@Component
public class AuthInterceptor {
    @Resource
    UserService userService;

    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint , AuthCheck authCheck) throws Throwable {
        String mustRole = authCheck.mustRole();
        UserRoleEnum mustUserRole = UserRoleEnum.getEnumByValue(mustRole);
        // 不需要权限，直接放行
        if (mustUserRole == null) {
            return joinPoint.proceed();
        }
        // 如果需要权限，则判断当前用户是否为管理员
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
        // 获取当前登录用户
        User currentUser = userService.getLoginUser(request);

        // 获取当前用户的角色
        String currentUserRole = currentUser.getUserRole();
        // 没有权限，拒绝访问
        if (!currentUserRole.equals(mustRole)){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 必须要求有权限，但是当前用户没有管理员权限，拒绝访问
        if (mustUserRole.equals(UserRoleEnum.ADMIN) && !currentUserRole.equals(UserRoleEnum.ADMIN.getValue())){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 校验通过，放行
        return joinPoint.proceed();
    }
}
