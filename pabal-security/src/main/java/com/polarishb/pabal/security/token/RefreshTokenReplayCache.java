package com.polarishb.pabal.security.token;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenReplayCache {

    Optional<IssuedTokenPair> findByRequest(String refreshTokenHash, String requestId);

    void saveByRequest(String refreshTokenHash, String requestId, IssuedTokenPair tokenPair, Duration ttl);

    Optional<IssuedTokenPair> findByUsedToken(UUID tokenId);

    void saveByUsedToken(UUID tokenId, IssuedTokenPair tokenPair, Duration ttl);
}
