package com.polarishb.pabal.security.token;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisRefreshTokenReplayCacheTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void findByUsedToken_returnsStoredTokenPair() throws Exception {
        UUID tokenId = UUID.randomUUID();
        IssuedTokenPair tokenPair = tokenPair();
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = valueOperations(redisTemplate);
        when(valueOperations.get("security:refresh:used:" + tokenId))
                .thenReturn(objectMapper.writeValueAsString(tokenPair));
        RedisRefreshTokenReplayCache cache = cache(redisTemplate);

        Optional<IssuedTokenPair> replay = cache.findByUsedToken(tokenId);

        assertThat(replay).contains(tokenPair);
    }

    @Test
    void saveByRequest_writesSerializedTokenWithTtl() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = valueOperations(redisTemplate);
        RedisRefreshTokenReplayCache cache = cache(redisTemplate);
        Duration ttl = Duration.ofSeconds(30);

        cache.saveByRequest("token-hash", "request-id", tokenPair(), ttl);

        verify(valueOperations).set(
                argThat(key -> key.startsWith("security:refresh:request:") && key.endsWith(":token-hash")),
                anyString(),
                eq(ttl)
        );
    }

    @Test
    void findByRequest_returnsEmptyWhenRedisIsUnavailable() {
        RedisRefreshTokenReplayCache cache = cache(null);

        Optional<IssuedTokenPair> replay = cache.findByRequest("token-hash", "request-id");

        assertThat(replay).isEmpty();
    }

    private RedisRefreshTokenReplayCache cache(StringRedisTemplate redisTemplate) {
        ObjectProvider<StringRedisTemplate> provider = mock();
        when(provider.getIfAvailable()).thenReturn(redisTemplate);
        return new RedisRefreshTokenReplayCache(provider, objectMapper);
    }

    @SuppressWarnings("unchecked")
    private ValueOperations<String, String> valueOperations(StringRedisTemplate redisTemplate) {
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        return valueOperations;
    }

    private IssuedTokenPair tokenPair() {
        Instant now = Instant.parse("2026-06-29T00:00:00Z");
        return new IssuedTokenPair(
                "Bearer",
                "access-token",
                now.plus(Duration.ofMinutes(60)),
                "refresh-token",
                now.plus(Duration.ofDays(7))
        );
    }
}
