package com.wjh.aicodegen.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @Author 王哈哈
 * @Date 2025/8/5 22:03:20
 * @Description  权限校验
 */
@Target(ElementType.METHOD)
@Retention(RUNTIME)
public @interface AuthCheck {
     /**
     * 必须有某个角色
     *
     * @return
     */
     String mustRole() default "";
}
