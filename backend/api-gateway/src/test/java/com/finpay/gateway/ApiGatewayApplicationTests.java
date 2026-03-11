package com.finpay.gateway;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class ApiGatewayApplicationTests {

    @Test
    void applicationClassExists() {
        assertDoesNotThrow(() -> Class.forName("com.finpay.gateway.ApiGatewayApplication"));
    }

}
