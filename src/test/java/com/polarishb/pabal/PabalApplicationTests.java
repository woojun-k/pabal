package com.polarishb.pabal;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.polarishb.pabal.support.AbstractPostgresIntegrationTest;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class PabalApplicationTests extends AbstractPostgresIntegrationTest {

    @Test
    void contextLoads() {
    }

}
