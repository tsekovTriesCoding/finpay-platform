package com.finpay.wallet.testconfig;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

import com.redis.testcontainers.RedisContainer;

/**
 * Shared Testcontainers configuration for wallet-service integration tests.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfig {

    @Bean
    @ServiceConnection(name = "mysql")
    public MySQLContainer mysqlContainer() {
        return new MySQLContainer(DockerImageName.parse("mysql:8.0"))
                .withDatabaseName("finpay_wallets_test")
                .withUsername("test")
                .withPassword("test")
                .withReuse(true);
    }

    @Bean
    @ServiceConnection(name = "kafka")
    public ConfluentKafkaContainer kafkaContainer() {
        return new ConfluentKafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"))
                .withReuse(true);
    }

    @Bean
    @ServiceConnection(name = "redis")
    public RedisContainer redisContainer() {
        return new RedisContainer(DockerImageName.parse("redis:7-alpine"))
                .withReuse(true);
    }
}
