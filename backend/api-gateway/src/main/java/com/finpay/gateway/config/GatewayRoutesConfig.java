package com.finpay.gateway.config;

import org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.setPath;
import static org.springframework.cloud.gateway.server.mvc.filter.LoadBalancerFilterFunctions.lb;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.predicate.GatewayRequestPredicates.path;

@Configuration
public class GatewayRoutesConfig {

    @Bean
    public RouterFunction<ServerResponse> authServiceRoute() {
        return route("auth-service")
                .route(path("/api/v1/auth/**"), HandlerFunctions.http())
                .filter(lb("auth-service"))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> oauth2AuthorizationRoute() {
        return route("oauth2-authorization")
                .route(path("/oauth2/**"), HandlerFunctions.http())
                .filter(lb("auth-service"))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> oauth2CallbackRoute() {
        return route("oauth2-callback")
                .route(path("/login/oauth2/**"), HandlerFunctions.http())
                .filter(lb("auth-service"))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> userServiceRoute() {
        return route("user-service")
                .route(path("/api/v1/users/**"), HandlerFunctions.http())
                .filter(lb("user-service"))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> paymentServiceRoute() {
        return route("payment-service")
                .route(path("/api/v1/payments/**"), HandlerFunctions.http())
                .filter(lb("payment-service"))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> walletServiceRoute() {
        return route("wallet-service")
                .route(path("/api/v1/wallets/**"), HandlerFunctions.http())
                .filter(lb("wallet-service"))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> transactionDetailRoute() {
        return route("transaction-detail")
                .route(path("/api/v1/transactions/**"), HandlerFunctions.http())
                .filter(lb("payment-service"))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> notificationServiceRoute() {
        return route("notification-service")
                .route(path("/api/v1/notifications/**"), HandlerFunctions.http())
                .filter(lb("notification-service"))
                .build();
    }

    // Admin Routes
    // These routes are additionally protected by AdminAuthFilter (ADMIN role check)

    @Bean
    public RouterFunction<ServerResponse> adminUserRoute() {
        return route("admin-users")
                .route(path("/api/v1/admin/users/**"), HandlerFunctions.http())
                .filter(lb("user-service"))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> adminTransactionRoute() {
        return route("admin-transactions")
                .route(path("/api/v1/admin/transactions/**"), HandlerFunctions.http())
                .filter(lb("payment-service"))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> adminWalletRoute() {
        return route("admin-wallets")
                .route(path("/api/v1/admin/wallets/**"), HandlerFunctions.http())
                .filter(lb("wallet-service"))
                .build();
    }

    // OpenAPI docs routes - proxy each service's /v3/api-docs for gateway aggregation

    @Bean
    public RouterFunction<ServerResponse> authServiceApiDocs() {
        return route("auth-service-api-docs")
                .route(path("/api-docs/auth-service"), HandlerFunctions.http())
                .before(setPath("/v3/api-docs"))
                .filter(lb("auth-service"))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> userServiceApiDocs() {
        return route("user-service-api-docs")
                .route(path("/api-docs/user-service"), HandlerFunctions.http())
                .before(setPath("/v3/api-docs"))
                .filter(lb("user-service"))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> paymentServiceApiDocs() {
        return route("payment-service-api-docs")
                .route(path("/api-docs/payment-service"), HandlerFunctions.http())
                .before(setPath("/v3/api-docs"))
                .filter(lb("payment-service"))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> walletServiceApiDocs() {
        return route("wallet-service-api-docs")
                .route(path("/api-docs/wallet-service"), HandlerFunctions.http())
                .before(setPath("/v3/api-docs"))
                .filter(lb("wallet-service"))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> notificationServiceApiDocs() {
        return route("notification-service-api-docs")
                .route(path("/api-docs/notification-service"), HandlerFunctions.http())
                .before(setPath("/v3/api-docs"))
                .filter(lb("notification-service"))
                .build();
    }
}
