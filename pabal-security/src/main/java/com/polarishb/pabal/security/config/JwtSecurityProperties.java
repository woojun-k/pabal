package com.polarishb.pabal.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "pabal.security.jwt")
public record JwtSecurityProperties(
    String issuerUri,
    String audience,
    String userIdClaim,
    String tenantIdClaim,
    String principalClaim,
    Duration clockSkew,
    Duration accessTokenMinTtl,
    Duration accessTokenMaxTtl,
    Duration refreshTokenTtl
) {

    public JwtSecurityProperties {
        if (clockSkew == null) {
            clockSkew = Duration.ofSeconds(30);
        }
        if (accessTokenMinTtl == null) {
            accessTokenMinTtl = Duration.ofMinutes(60);
        }
        if (accessTokenMaxTtl == null) {
            accessTokenMaxTtl = Duration.ofMinutes(90);
        }
        if (refreshTokenTtl == null) {
            refreshTokenTtl = Duration.ofDays(7);
        }
        if (accessTokenMinTtl.compareTo(Duration.ZERO) <= 0) {
            throw new IllegalArgumentException("accessTokenMinTtl must be positive");
        }
        if (accessTokenMaxTtl.compareTo(accessTokenMinTtl) < 0) {
            throw new IllegalArgumentException("accessTokenMaxTtl must be greater than or equal to accessTokenMinTtl");
        }
    }
}
