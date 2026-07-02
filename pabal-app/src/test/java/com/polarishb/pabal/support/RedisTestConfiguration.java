package com.polarishb.pabal.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;

@TestConfiguration(proxyBeanMethods = false)
public class RedisTestConfiguration {

    @Bean
    @ServiceConnection(name = "redis")
    GenericContainer<?> redisContainer() {
        @SuppressWarnings("resource")
        GenericContainer<?> container = new GenericContainer<>("redis:8.6-alpine")
                .withExposedPorts(6379);

        return container;
    }
}