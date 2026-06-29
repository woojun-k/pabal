package com.polarishb.pabal.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "pabal.security.refresh-token")
public record RefreshTokenReplayProperties(
        Duration reuseGracePeriod,
        Duration requestIdempotencyTtl
) {

    public RefreshTokenReplayProperties {
        if (reuseGracePeriod == null) {
            reuseGracePeriod = Duration.ofSeconds(3);
        }
        if (requestIdempotencyTtl == null) {
            requestIdempotencyTtl = Duration.ofSeconds(30);
        }
        if (reuseGracePeriod.compareTo(Duration.ZERO) < 0) {
            throw new IllegalArgumentException("reuseGracePeriod must not be negative");
        }
        if (requestIdempotencyTtl.compareTo(Duration.ZERO) <= 0) {
            throw new IllegalArgumentException("requestIdempotencyTtl must be positive");
        }
    }
}
