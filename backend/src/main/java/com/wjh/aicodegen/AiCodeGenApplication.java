package com.wjh.aicodegen;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * @author 王哈哈
 */
// 启动时开启aop
@EnableAspectJAutoProxy(exposeProxy = true)
// 开启使用缓存，支持使用 Spring Date 缓存注解 @Cacheable
@EnableCaching
@MapperScan({"com.wjh.aicodegen.mapper"})
@SpringBootApplication
@Slf4j
public class AiCodeGenApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiCodeGenApplication.class, args);
        log.info("ai-code-gen 项目 启动成功");
    }
}