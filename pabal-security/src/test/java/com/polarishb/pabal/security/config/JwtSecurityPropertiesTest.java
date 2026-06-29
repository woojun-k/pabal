package com.polarishb.pabal.security.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtSecurityPropertiesTest {

    @Test
    void defaults_access_token_ttl_range_to_sixty_to_ninety_minutes() {
        JwtSecurityProperties properties = new JwtSecurityProperties(
                "issuer",
                "pabal-api",
                "uid",
                "tenant_id",
                "sub",
                null,
                null,
                null,
                null
        );

        assertThat(properties.clockSkew()).isEqualTo(Duration.ofSeconds(30));
        assertThat(properties.accessTokenMinTtl()).isEqualTo(Duration.ofMinutes(60));
        assertThat(properties.accessTokenMaxTtl()).isEqualTo(Duration.ofMinutes(90));
        assertThat(properties.refreshTokenTtl()).isEqualTo(Duration.ofDays(7));
    }

    @Test
    void rejects_reversed_access_token_ttl_range() {
        assertThatThrownBy(() -> new JwtSecurityProperties(
                "issuer",
                "pabal-api",
                "uid",
                "tenant_id",
                "sub",
                Duration.ofSeconds(30),
                Duration.ofMinutes(90),
                Duration.ofMinutes(60),
                Duration.ofDays(7)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("accessTokenMaxTtl");
    }

    @Test
    void rejects_non_positive_min_access_token_ttl() {
        assertThatThrownBy(() -> new JwtSecurityProperties(
                "issuer",
                "pabal-api",
                "uid",
                "tenant_id",
                "sub",
                Duration.ofSeconds(30),
                Duration.ZERO,
                Duration.ofMinutes(90),
                Duration.ofDays(7)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("accessTokenMinTtl");
    }
}
