package com.finpay.payment.testconfig;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * MySQL-only Testcontainers configuration for @DataJpaTest repository tests.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestMySQLContainerConfig {

    @Bean
    @ServiceConnection(name = "mysql")
    public MySQLContainer mysqlContainer() {
        return new MySQLContainer(DockerImageName.parse("mysql:8.0"))
                .withDatabaseName("finpay_payments_test")
                .withUsername("test")
                .withPassword("test")
                .withReuse(true);
    }
}
