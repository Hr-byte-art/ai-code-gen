package com.wjh.aicodegen.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.jackson.JsonComponent;
import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * Spring MVC Json 配置
 *
 * @author 木子宸
 */
@JsonComponent
public class JsonConfig {

    /**
     * 添加 Long 转 json 精度丢失的配置
     *
     * @param builder Jackson对象映射器构建器
     * @return 配置后的ObjectMapper实例
     */
    @Bean
    public ObjectMapper jacksonObjectMapper(Jackson2ObjectMapperBuilder builder) {
        // 创建ObjectMapper实例，不创建XML映射器
        ObjectMapper objectMapper = builder.createXmlMapper(false).build();

        // 注册自定义模块，解决Long类型转换为JSON时的精度丢失问题
        SimpleModule module = new SimpleModule();
        //为Long包装类型添加序列化器
        module.addSerializer(Long.class, ToStringSerializer.instance);
        //为long基本数据类型添加序列化器
        module.addSerializer(Long.TYPE, ToStringSerializer.instance);
        objectMapper.registerModule(module);

        return objectMapper;
    }

}
