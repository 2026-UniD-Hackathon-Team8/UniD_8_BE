package com.unid8.damideo.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String DEVICE_ID = "X-Device-Id";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("다믿어? API")
                        .description("게스트 계정 생성 외 모든 요청은 X-Device-Id 헤더가 필요합니다.")
                        .version("v1"))
                .components(new Components()
                        .addSecuritySchemes(DEVICE_ID, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name(DEVICE_ID)))
                .addSecurityItem(new SecurityRequirement().addList(DEVICE_ID));
    }
}
