package com.finpay.notification.shared.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI notificationServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("FinPay Notification Service API")
                        .description("Notification service - email, in-app, and push notifications")
                        .version("1.0.0")
                        .contact(new Contact().name("FinPay Team"))
                        .license(new License().name("MIT")));
    }
}
