package com.wjh.aicodegen.convert;

import com.wjh.aicodegen.model.dto.vipCode.VipCodeAddRequest;
import com.wjh.aicodegen.model.entity.VipCode;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-28T23:23:16+0800",
    comments = "version: 1.6.3, compiler: javac, environment: Java 21.0.10 (Microsoft)"
)
@Component
public class VipCodeConverterImpl implements VipCodeConverter {

    @Override
    public VipCode toEntity(VipCodeAddRequest vipCodeAddRequest) {
        if ( vipCodeAddRequest == null ) {
            return null;
        }

        VipCode.VipCodeBuilder vipCode = VipCode.builder();

        vipCode.vipCode( vipCodeAddRequest.getVipCode() );
        vipCode.effectiveDay( vipCodeAddRequest.getEffectiveDay() );
        vipCode.expDate( vipCodeAddRequest.getExpDate() );
        vipCode.maxUseNum( vipCodeAddRequest.getMaxUseNum() );

        return vipCode.build();
    }
}
