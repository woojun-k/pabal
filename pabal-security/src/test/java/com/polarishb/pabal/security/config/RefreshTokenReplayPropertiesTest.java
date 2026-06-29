package com.polarishb.pabal.security.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RefreshTokenReplayPropertiesTest {

    @Test
    void defaults_grace_to_three_seconds_and_request_idempotency_to_thirty_seconds() {
        RefreshTokenReplayProperties properties = new RefreshTokenReplayProperties(null, null);

        assertThat(properties.reuseGracePeriod()).isEqualTo(Duration.ofSeconds(3));
        assertThat(properties.requestIdempotencyTtl()).isEqualTo(Duration.ofSeconds(30));
    }

    @Test
    void rejects_negative_grace_period() {
        assertThatThrownBy(() -> new RefreshTokenReplayProperties(
                Duration.ofSeconds(-1),
                Duration.ofSeconds(30)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("reuseGracePeriod");
    }

    @Test
    void rejects_non_positive_request_idempotency_ttl() {
        assertThatThrownBy(() -> new RefreshTokenReplayProperties(
                Duration.ofSeconds(30),
                Duration.ZERO
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("requestIdempotencyTtl");
    }
}
