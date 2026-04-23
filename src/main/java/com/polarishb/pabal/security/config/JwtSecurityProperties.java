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
    Duration clockSkew
) {
}
