package com.finpay.user.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI userServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("FinPay User Service API")
                        .description("User management service - profiles, search, avatars, admin operations")
                        .version("1.0.0")
                        .contact(new Contact().name("FinPay Team"))
                        .license(new License().name("MIT")));
    }
}
