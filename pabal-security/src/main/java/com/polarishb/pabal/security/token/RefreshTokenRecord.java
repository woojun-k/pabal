package com.polarishb.pabal.security.token;

import java.time.Instant;
import java.util.UUID;

public record RefreshTokenRecord(
        UUID id,
        String tokenHash,
        UUID tenantId,
        UUID userId,
        String subject,
        JwtAuthorityClaims authorityClaims,
        Instant issuedAt,
        Instant usedAt,
        Instant expiresAt,
        Instant revokedAt,
        UUID replacedByTokenId
) {

    public boolean isActive(Instant now) {
        return revokedAt == null && expiresAt.isAfter(now);
    }
}
