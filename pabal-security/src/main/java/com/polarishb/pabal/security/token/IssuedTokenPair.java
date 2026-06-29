package com.polarishb.pabal.security.token;

import java.time.Instant;

public record IssuedTokenPair(
        String tokenType,
        String accessToken,
        Instant accessTokenExpiresAt,
        String refreshToken,
        Instant refreshTokenExpiresAt
) {
}
