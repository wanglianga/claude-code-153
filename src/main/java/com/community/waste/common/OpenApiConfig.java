package com.community.waste.common;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("小区装修垃圾预约清运称重计费投诉追责服务 API")
                        .version("1.0.0")
                        .description("覆盖：装修登记、每日堆放上报与审核、容量规则（错峰/临时围挡）、"
                                + "清运预约与派单（门禁/限行/容量/已有派单）、进出场称重与联单、"
                                + "处置场回执、违规与投诉追责、押金扣款、费用争议、四方账单结算、"
                                + "单袋垃圾全链路追溯。\n\n"
                                + "鉴权：所有 /api/** 接口需请求头 X-Auth-Token，"
                                + "取值：property-token / company-token / finance-token / crew-token / owner-token。"))
                .addSecurityItem(new SecurityRequirement().addList("X-Auth-Token"))
                .components(new Components().addSecuritySchemes("X-Auth-Token",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-Auth-Token")));
    }
}
