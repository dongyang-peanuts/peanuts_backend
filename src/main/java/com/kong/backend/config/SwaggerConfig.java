package com.kong.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/* http://localhost:8080/swagger-ui/index.html */
/* http://kongback.kro.kr:8080/swagger-ui/index.html */
@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("DDANG_KONG API 문서")
                        .description("회원가입 / 로그인 / 로그아웃 / Websoket")
                        .version("v1.0"));
    }
}