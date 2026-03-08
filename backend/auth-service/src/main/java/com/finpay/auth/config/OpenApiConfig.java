package com.finpay.auth.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI authServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("FinPay Auth Service API")
                        .description("Authentication and authorization service - registration, login, JWT tokens, OAuth2")
                        .version("1.0.0")
                        .contact(new Contact().name("FinPay Team"))
                        .license(new License().name("MIT")));
    }
}
