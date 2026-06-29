package com.polarishb.pabal.security.token;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
public class RedisRefreshTokenReplayCache implements RefreshTokenReplayCache {

    private static final String REQUEST_KEY_PREFIX = "security:refresh:request:";
    private static final String USED_TOKEN_KEY_PREFIX = "security:refresh:used:";

    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;
    private final JsonMapper jsonMapper;

    public RedisRefreshTokenReplayCache(
            ObjectProvider<StringRedisTemplate> redisTemplateProvider,
            JsonMapper jsonMapper
    ) {
        this.redisTemplateProvider = Objects.requireNonNull(
                redisTemplateProvider,
                "redisTemplateProvider must not be null"
        );
        this.jsonMapper = Objects.requireNonNull(jsonMapper, "jsonMapper must not be null");
    }

    @Override
    public Optional<IssuedTokenPair> findByRequest(String refreshTokenHash, String requestId) {
        return find(requestKey(refreshTokenHash, requestId));
    }

    @Override
    public void saveByRequest(String refreshTokenHash, String requestId, IssuedTokenPair tokenPair, Duration ttl) {
        save(requestKey(refreshTokenHash, requestId), tokenPair, ttl);
    }

    @Override
    public Optional<IssuedTokenPair> findByUsedToken(UUID tokenId) {
        return find(usedTokenKey(tokenId));
    }

    @Override
    public void saveByUsedToken(UUID tokenId, IssuedTokenPair tokenPair, Duration ttl) {
        save(usedTokenKey(tokenId), tokenPair, ttl);
    }

    private Optional<IssuedTokenPair> find(String key) {
        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate == null) {
            return Optional.empty();
        }

        try {
            String value = redisTemplate.opsForValue().get(key);
            if (value == null) {
                return Optional.empty();
            }
            return Optional.of(jsonMapper.readValue(value, IssuedTokenPair.class));
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }

    private void save(String key, IssuedTokenPair tokenPair, Duration ttl) {
        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate == null || ttl.isZero() || ttl.isNegative()) {
            return;
        }

        try {
            redisTemplate.opsForValue().set(key, jsonMapper.writeValueAsString(tokenPair), ttl);
        } catch (RuntimeException ignored) {
            // Replay cache failures must not change refresh token truth; DB state remains authoritative.
        }
    }

    private String requestKey(String refreshTokenHash, String requestId) {
        return REQUEST_KEY_PREFIX + sha256(requestId) + ":" + refreshTokenHash;
    }

    private String usedTokenKey(UUID tokenId) {
        return USED_TOKEN_KEY_PREFIX + tokenId;
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}
