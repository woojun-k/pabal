package com.polarishb.pabal.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

@TestConfiguration(proxyBeanMethods = false)
public class PostgresTestConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        @SuppressWarnings("resource")
        PostgreSQLContainer container = new PostgreSQLContainer("postgres:18.3")
                .withDatabaseName("pabal_test")
                .withUsername("test")
                .withPassword("test");

        return container;
    }
}
