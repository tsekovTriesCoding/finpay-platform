package com.finpay.registry;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=" +
                "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration," +
                "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration," +
                "org.springframework.boot.data.jpa.autoconfigure.JpaRepositoriesAutoConfiguration"
})
class ServiceRegistryApplicationTests {

    @Test
    void contextLoads() {
    }

}
