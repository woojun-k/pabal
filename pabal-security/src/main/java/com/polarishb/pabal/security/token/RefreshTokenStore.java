package com.polarishb.pabal.security.token;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenStore {

    void save(RefreshTokenRecord token);

    Optional<RefreshTokenRecord> findByTokenHash(String tokenHash);

    boolean markUsedAndRevoke(UUID tokenId, UUID replacedByTokenId, Instant usedAt);

    boolean revoke(UUID tokenId, UUID replacedByTokenId, Instant revokedAt);

    void revokeTokenFamily(UUID rootTokenId, Instant revokedAt);

    void revokeUserTokens(UUID tenantId, UUID userId, Instant revokedAt);
}
