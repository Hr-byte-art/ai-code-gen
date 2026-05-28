package com.wjh.aicodegen.convert;

import com.wjh.aicodegen.model.dto.app.AppAddRequest;
import com.wjh.aicodegen.model.dto.app.AppAdminUpdateRequest;
import com.wjh.aicodegen.model.entity.App;
import com.wjh.aicodegen.model.vo.app.AppVO;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-28T23:23:16+0800",
    comments = "version: 1.6.3, compiler: javac, environment: Java 21.0.10 (Microsoft)"
)
@Component
public class AppConverterImpl implements AppConverter {

    @Override
    public App toApp(AppAddRequest appAddRequest) {
        if ( appAddRequest == null ) {
            return null;
        }

        App.AppBuilder app = App.builder();

        app.initPrompt( appAddRequest.getInitPrompt() );
        app.codeGenType( appAddRequest.getCodeGenType() );

        return app.build();
    }

    @Override
    public AppVO toAppVO(App app) {
        if ( app == null ) {
            return null;
        }

        AppVO appVO = new AppVO();

        appVO.setId( app.getId() );
        appVO.setAppName( app.getAppName() );
        appVO.setCover( app.getCover() );
        appVO.setInitPrompt( app.getInitPrompt() );
        appVO.setCodeGenType( app.getCodeGenType() );
        appVO.setDeployKey( app.getDeployKey() );
        appVO.setDeployedTime( app.getDeployedTime() );
        appVO.setPriority( app.getPriority() );
        appVO.setUserId( app.getUserId() );
        appVO.setCreateTime( app.getCreateTime() );
        appVO.setUpdateTime( app.getUpdateTime() );

        return appVO;
    }

    @Override
    public App toApp(AppAdminUpdateRequest appAdminUpdateRequest) {
        if ( appAdminUpdateRequest == null ) {
            return null;
        }

        App.AppBuilder app = App.builder();

        app.id( appAdminUpdateRequest.getId() );
        app.appName( appAdminUpdateRequest.getAppName() );
        app.cover( appAdminUpdateRequest.getCover() );
        app.priority( appAdminUpdateRequest.getPriority() );

        return app.build();
    }
}
