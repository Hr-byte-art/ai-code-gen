package com.wjh.aicodegen.convert;

import com.wjh.aicodegen.model.dto.app.AppAddRequest;
import com.wjh.aicodegen.model.dto.app.AppAdminUpdateRequest;
import com.wjh.aicodegen.model.entity.App;
import com.wjh.aicodegen.model.vo.app.AppVO;
import org.mapstruct.Mapper;

/**
 * @Author 王哈哈
 * @Date 2025/8/8 00:36:15
 * @Description 应用模型转换器
 */
@Mapper(componentModel = "spring")
public interface AppConverter {
    App toApp(AppAddRequest appAddRequest);
    AppVO toAppVO(App app);
    App toApp(AppAdminUpdateRequest appAdminUpdateRequest);
}
