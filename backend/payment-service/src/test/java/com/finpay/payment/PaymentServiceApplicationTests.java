package com.finpay.payment;

import com.finpay.payment.testconfig.TestcontainersConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfig.class)
class PaymentServiceApplicationTests {

    @Test
    void contextLoads() {
    }

}
