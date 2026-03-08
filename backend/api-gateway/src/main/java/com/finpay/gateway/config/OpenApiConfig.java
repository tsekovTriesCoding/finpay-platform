package com.finpay.gateway.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI gatewayOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("FinPay Platform API")
                        .description("Aggregated API documentation for all FinPay microservices. "
                                + "Use the dropdown above to switch between services.")
                        .version("1.0.0")
                        .contact(new Contact().name("FinPay Team"))
                        .license(new License().name("MIT")));
    }
}
