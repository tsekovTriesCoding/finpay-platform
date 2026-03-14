package com.finpay.user;

import com.finpay.user.testconfig.TestcontainersConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfig.class)
class UserServiceApplicationTests {

    @Test
    void contextLoads() {
    }

}
