package com.wjh.aicodegen.config;

import com.wjh.aicodegen.constant.AppConstant;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowCredentials(true)
                // 仅允许已知域名，不要用 "*"（存在 CSRF 风险）
                .allowedOriginPatterns(
                        "http://localhost:*",
                        "http://127.0.0.1:*",
                        "https://entropyqing.asia",
                        "https://*.entropyqing.asia"
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("*");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String deployLocation = new File(AppConstant.CODE_DEPLOY_ROOT_DIR).toURI().toString();
        registry.addResourceHandler("/code_deploy/**")
                .addResourceLocations(deployLocation);
    }
}
