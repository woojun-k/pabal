package com.polarishb.pabal.security.token;

import com.polarishb.pabal.security.config.JwtSecurityProperties;
import com.polarishb.pabal.security.config.RefreshTokenReplayProperties;
import com.polarishb.pabal.security.time.ClockPort;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int REFRESH_TOKEN_BYTE_LENGTH = 32;
    private static final int REFRESH_TOKEN_LENGTH = 43;
    private static final int MAX_REQUEST_ID_LENGTH = 128;
    private static final Pattern REFRESH_TOKEN_PATTERN =
            Pattern.compile("[A-Za-z0-9_-]{" + REFRESH_TOKEN_LENGTH + "}");

    private final JwtSecurityProperties jwtProperties;
    private final RefreshTokenReplayProperties replayProperties;
    private final ObjectProvider<JwtEncoder> jwtEncoderProvider;
    private final RefreshTokenStore refreshTokenStore;
    private final RefreshTokenReplayCache replayCache;
    private final ClockPort clockPort;

    @Transactional
    public IssuedTokenPair issueTokenPair(
            UUID userId,
            UUID tenantId,
            String subject,
            JwtAuthorityClaims authorityClaims
    ) {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(subject, "subject must not be null");
        Objects.requireNonNull(authorityClaims, "authorityClaims must not be null");

        Instant now = clockPort.now();
        AccessTokenIssue accessToken = issueAccessToken(userId, tenantId, subject, authorityClaims, now);
        RefreshTokenIssue refreshToken = issueRefreshToken(userId, tenantId, subject, authorityClaims, now);

        return new IssuedTokenPair(
                "Bearer",
                accessToken.tokenValue(),
                accessToken.expiresAt(),
                refreshToken.rawToken(),
                refreshToken.expiresAt()
        );
    }

    @Transactional
    public IssuedTokenPair refresh(String rawRefreshToken) {
        return refresh(rawRefreshToken, null);
    }

    @Transactional
    public IssuedTokenPair refresh(String rawRefreshToken, String requestId) {
        String tokenHash = hash(rawRefreshToken);
        Optional<String> normalizedRequestId = normalizeRequestId(requestId);
        Optional<IssuedTokenPair> requestReplay = normalizedRequestId
                .flatMap(id -> replayCache.findByRequest(tokenHash, id));
        if (requestReplay.isPresent()) {
            return requestReplay.get();
        }

        RefreshTokenRecord current = refreshTokenStore.findByTokenHash(tokenHash)
                .orElseThrow(() -> new InvalidRefreshTokenException("Refresh token was not found"));

        Instant now = clockPort.now();
        if (!current.isActive(now)) {
            Optional<IssuedTokenPair> graceReplay = findGraceReplay(current, now);
            if (graceReplay.isPresent()) {
                cacheRequestReplay(tokenHash, normalizedRequestId, graceReplay.get());
                return graceReplay.get();
            }
            if (isGraceReplayAllowed(current, now)) {
                throw new InvalidRefreshTokenException("Refresh token replay is not available");
            }
            revokeReusedTokenFamily(current, now);
            throw new InvalidRefreshTokenException("Refresh token is expired or revoked");
        }

        RefreshTokenIssue next = issueRefreshToken(
                current.userId(),
                current.tenantId(),
                current.subject(),
                current.authorityClaims(),
                now
        );
        boolean revoked;
        try {
            revoked = refreshTokenStore.markUsedAndRevoke(current.id(), next.id(), now);
        } catch (RuntimeException ex) {
            revokePreIssuedRefreshToken(next.id(), now);
            throw ex;
        }
        if (!revoked) {
            revokePreIssuedRefreshToken(next.id(), now);
            Optional<RefreshTokenRecord> updated = refreshTokenStore.findByTokenHash(tokenHash);
            if (updated.isPresent()) {
                Optional<IssuedTokenPair> graceReplay = findGraceReplay(updated.get(), now);
                if (graceReplay.isPresent()) {
                    cacheRequestReplay(tokenHash, normalizedRequestId, graceReplay.get());
                    return graceReplay.get();
                }
                if (isGraceReplayAllowed(updated.get(), now)) {
                    throw new InvalidRefreshTokenException("Refresh token replay is not available");
                }
            }
            revokeRefreshTokenFamily(current.id(), now);
            throw new InvalidRefreshTokenException("Refresh token was already used");
        }

        AccessTokenIssue accessToken = issueAccessToken(
                current.userId(),
                current.tenantId(),
                current.subject(),
                current.authorityClaims(),
                now
        );

        IssuedTokenPair tokenPair = new IssuedTokenPair(
                "Bearer",
                accessToken.tokenValue(),
                accessToken.expiresAt(),
                next.rawToken(),
                next.expiresAt()
        );
        cacheReplay(current.id(), tokenHash, normalizedRequestId, tokenPair);
        return tokenPair;
    }

    @Transactional
    public void revoke(String rawRefreshToken) {
        String tokenHash = hash(rawRefreshToken);
        refreshTokenStore.findByTokenHash(tokenHash)
                .ifPresent(token -> refreshTokenStore.revoke(token.id(), null, clockPort.now()));
    }

    @Transactional
    public void revokeUserTokens(UUID tenantId, UUID userId) {
        refreshTokenStore.revokeUserTokens(tenantId, userId, clockPort.now());
    }

    private AccessTokenIssue issueAccessToken(
            UUID userId,
            UUID tenantId,
            String subject,
            JwtAuthorityClaims authorityClaims,
            Instant now
    ) {
        JwtEncoder jwtEncoder = jwtEncoderProvider.getIfAvailable();
        if (jwtEncoder == null) {
            throw new IllegalStateException("JwtEncoder is not configured for access token issuance");
        }

        Instant expiresAt = now.plus(randomAccessTokenTtl());
        JwtClaimsSet.Builder claimsBuilder = JwtClaimsSet.builder()
                .issuer(jwtProperties.issuerUri())
                .subject(subject)
                .issuedAt(now)
                .expiresAt(expiresAt)
                .audience(List.of(jwtProperties.audience()))
                .claim(jwtProperties.userIdClaim(), userId.toString())
                .claim(jwtProperties.tenantIdClaim(), tenantId.toString())
                .claim(jwtProperties.principalClaim(), subject);

        if (!authorityClaims.scopes().isEmpty()) {
            claimsBuilder.claim("scope", String.join(" ", authorityClaims.scopes()));
        }
        if (!authorityClaims.roles().isEmpty()) {
            claimsBuilder.claim("roles", authorityClaims.roles());
        }
        if (!authorityClaims.permissions().isEmpty()) {
            claimsBuilder.claim("permissions", authorityClaims.permissions());
        }

        String tokenValue = jwtEncoder.encode(
                JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claimsBuilder.build())
        ).getTokenValue();
        return new AccessTokenIssue(tokenValue, expiresAt);
    }

    private Duration randomAccessTokenTtl() {
        Duration minTtl = jwtProperties.accessTokenMinTtl();
        Duration maxTtl = jwtProperties.accessTokenMaxTtl();
        if (maxTtl.compareTo(minTtl) < 0) {
            throw new IllegalStateException("Access token max TTL must be greater than or equal to min TTL");
        }
        long rangeSeconds = maxTtl.minus(minTtl).toSeconds();
        if (rangeSeconds == 0) {
            return minTtl;
        }
        long randomOffsetSeconds = SECURE_RANDOM.nextLong(rangeSeconds + 1);
        return minTtl.plusSeconds(randomOffsetSeconds);
    }

    private RefreshTokenIssue issueRefreshToken(
            UUID userId,
            UUID tenantId,
            String subject,
            JwtAuthorityClaims authorityClaims,
            Instant now
    ) {
        UUID id = UUID.randomUUID();
        String rawToken = newOpaqueToken();
        Instant expiresAt = now.plus(jwtProperties.refreshTokenTtl());
        RefreshTokenRecord record = new RefreshTokenRecord(
                id,
                hash(rawToken),
                tenantId,
                userId,
                subject,
                authorityClaims,
                now,
                null,
                expiresAt,
                null,
                null
        );
        refreshTokenStore.save(record);
        return new RefreshTokenIssue(id, rawToken, expiresAt);
    }

    private String newOpaqueToken() {
        byte[] bytes = new byte[REFRESH_TOKEN_BYTE_LENGTH];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String rawToken) {
        validateRefreshTokenFormat(rawToken);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    private void validateRefreshTokenFormat(String rawToken) {
        if (rawToken == null || !REFRESH_TOKEN_PATTERN.matcher(rawToken).matches()) {
            throw new InvalidRefreshTokenException("Invalid refresh token format");
        }
    }

    private Optional<String> normalizeRequestId(String requestId) {
        if (requestId == null) {
            return Optional.empty();
        }
        String normalized = requestId.trim();
        if (normalized.isBlank() || normalized.length() > MAX_REQUEST_ID_LENGTH) {
            return Optional.empty();
        }
        boolean hasControlCharacter = normalized.chars().anyMatch(Character::isISOControl);
        return hasControlCharacter ? Optional.empty() : Optional.of(normalized);
    }

    private Optional<IssuedTokenPair> findGraceReplay(RefreshTokenRecord token, Instant now) {
        if (!isGraceReplayAllowed(token, now)) {
            return Optional.empty();
        }
        return replayCache.findByUsedToken(token.id());
    }

    private boolean isGraceReplayAllowed(RefreshTokenRecord token, Instant now) {
        Duration gracePeriod = replayProperties.reuseGracePeriod();
        return !gracePeriod.isZero()
                && token.usedAt() != null
                && token.replacedByTokenId() != null
                && !now.isAfter(token.usedAt().plus(gracePeriod));
    }

    private void cacheReplay(
            UUID usedTokenId,
            String refreshTokenHash,
            Optional<String> requestId,
            IssuedTokenPair tokenPair
    ) {
        replayCache.saveByUsedToken(usedTokenId, tokenPair, replayProperties.reuseGracePeriod());
        cacheRequestReplay(refreshTokenHash, requestId, tokenPair);
    }

    private void cacheRequestReplay(
            String refreshTokenHash,
            Optional<String> requestId,
            IssuedTokenPair tokenPair
    ) {
        requestId.ifPresent(id -> replayCache.saveByRequest(
                refreshTokenHash,
                id,
                tokenPair,
                replayProperties.requestIdempotencyTtl()
        ));
    }

    private void revokePreIssuedRefreshToken(UUID tokenId, Instant revokedAt) {
        try {
            refreshTokenStore.revoke(tokenId, null, revokedAt);
        } catch (RuntimeException ex) {
            log.warn("Failed to revoke pre-issued refresh token after refresh rotation failure", ex);
        }
    }

    private void revokeReusedTokenFamily(RefreshTokenRecord current, Instant revokedAt) {
        // Naturally expired tokens are not treated as token reuse compromise.
        if (current.revokedAt() == null) {
            return;
        }
        if (current.replacedByTokenId() == null) {
            return;
        }
        revokeRefreshTokenFamily(current.id(), revokedAt);
    }

    private void revokeRefreshTokenFamily(UUID rootTokenId, Instant revokedAt) {
        try {
            refreshTokenStore.revokeTokenFamily(rootTokenId, revokedAt);
        } catch (RuntimeException ex) {
            log.warn("Failed to revoke refresh token family after refresh token reuse detection", ex);
        }
    }

    private record RefreshTokenIssue(
            UUID id,
            String rawToken,
            Instant expiresAt
    ) {
    }

    private record AccessTokenIssue(
            String tokenValue,
            Instant expiresAt
    ) {
    }
}
