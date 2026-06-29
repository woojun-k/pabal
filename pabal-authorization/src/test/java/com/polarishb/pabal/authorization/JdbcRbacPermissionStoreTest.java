package com.polarishb.pabal.authorization;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class JdbcRbacPermissionStoreTest {

    private static final Duration CACHE_TTL = Duration.ofMinutes(5);
    private static final String CACHE_MARKER = "__pabal_rbac_permissions_cached__";

    @Test
    void findPermissionValues_queriesDatabaseAndCachesOnRedisMiss() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String cacheKey = cacheKey(tenantId, userId);
        JdbcClient jdbcClient = jdbcClientReturning(List.of(
                "messenger:room:invite",
                "messenger:channel:create"
        ));
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        SetOperations<String, String> setOperations = setOperations(redisTemplate);
        when(redisTemplate.hasKey(cacheKey)).thenReturn(false);
        JdbcRbacPermissionStore store = store(jdbcClient, redisTemplate);

        Set<String> permissions = store.findPermissionValues(tenantId, userId);

        assertThat(permissions).containsExactlyInAnyOrder(
                "messenger:room:invite",
                "messenger:channel:create"
        );
        verify(jdbcClient).sql(anyString());
        verify(redisTemplate).delete(cacheKey);
        verify(setOperations).add(eq(cacheKey), any(String[].class));
        verify(redisTemplate).expire(cacheKey, CACHE_TTL);
    }

    @Test
    void findPermissionValues_returnsRedisHitWithoutQueryingDatabase() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String cacheKey = cacheKey(tenantId, userId);
        JdbcClient jdbcClient = mock(JdbcClient.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        SetOperations<String, String> setOperations = setOperations(redisTemplate);
        when(redisTemplate.hasKey(cacheKey)).thenReturn(true);
        when(setOperations.members(cacheKey)).thenReturn(Set.of(CACHE_MARKER, "messenger:room:invite"));
        JdbcRbacPermissionStore store = store(jdbcClient, redisTemplate);

        Set<String> permissions = store.findPermissionValues(tenantId, userId);

        assertThat(permissions).containsExactly("messenger:room:invite");
        verifyNoInteractions(jdbcClient);
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    void findPermissionValues_fallsBackToDatabaseWhenRedisThrows() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String cacheKey = cacheKey(tenantId, userId);
        JdbcClient jdbcClient = jdbcClientReturning(List.of("messenger:room:invite"));
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        when(redisTemplate.hasKey(cacheKey)).thenThrow(new IllegalStateException("redis unavailable"));
        JdbcRbacPermissionStore store = store(jdbcClient, redisTemplate);

        Set<String> permissions = store.findPermissionValues(tenantId, userId);

        assertThat(permissions).containsExactly("messenger:room:invite");
        verify(jdbcClient).sql(anyString());
    }

    @Test
    void findPermissionValues_cachesEmptyPermissionSetWithMarker() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String cacheKey = cacheKey(tenantId, userId);
        JdbcClient jdbcClient = jdbcClientReturning(List.of());
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        SetOperations<String, String> setOperations = setOperations(redisTemplate);
        when(redisTemplate.hasKey(cacheKey)).thenReturn(false);
        JdbcRbacPermissionStore store = store(jdbcClient, redisTemplate);

        Set<String> permissions = store.findPermissionValues(tenantId, userId);

        assertThat(permissions).isEmpty();
        verify(setOperations).add(cacheKey, CACHE_MARKER);
        verify(redisTemplate).expire(cacheKey, CACHE_TTL);
    }

    @Test
    void evictPermissionValues_deletesRedisCacheKey() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        JdbcRbacPermissionStore store = store(mock(JdbcClient.class), redisTemplate);

        store.evictPermissionValues(tenantId, userId);

        verify(redisTemplate).delete(cacheKey(tenantId, userId));
    }

    @Test
    void findPermissionValues_treatsNullRedisMembersAsCacheMiss() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String cacheKey = cacheKey(tenantId, userId);
        JdbcClient jdbcClient = jdbcClientReturning(List.of("messenger:room:invite"));
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        SetOperations<String, String> setOperations = setOperations(redisTemplate);
        when(redisTemplate.hasKey(cacheKey)).thenReturn(true);
        when(setOperations.members(cacheKey)).thenReturn(null);
        JdbcRbacPermissionStore store = store(jdbcClient, redisTemplate);

        Set<String> permissions = store.findPermissionValues(tenantId, userId);

        assertThat(permissions).containsExactly("messenger:room:invite");
        verify(jdbcClient).sql(anyString());
        verify(redisTemplate).delete(cacheKey);
    }

    private JdbcRbacPermissionStore store(JdbcClient jdbcClient, StringRedisTemplate redisTemplate) {
        ObjectProvider<StringRedisTemplate> redisTemplateProvider = mock();
        when(redisTemplateProvider.getIfAvailable()).thenReturn(redisTemplate);
        return new JdbcRbacPermissionStore(jdbcClient, redisTemplateProvider);
    }

    private JdbcClient jdbcClientReturning(List<String> permissionValues) {
        JdbcClient jdbcClient = mock(JdbcClient.class);
        JdbcClient.StatementSpec statementSpec = mock(JdbcClient.StatementSpec.class);
        JdbcClient.MappedQuerySpec<String> querySpec = mock();
        when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
        when(statementSpec.param(anyString(), any())).thenReturn(statementSpec);
        when(statementSpec.query(String.class)).thenReturn(querySpec);
        when(querySpec.list()).thenReturn(permissionValues);
        return jdbcClient;
    }

    @SuppressWarnings("unchecked")
    private SetOperations<String, String> setOperations(StringRedisTemplate redisTemplate) {
        SetOperations<String, String> setOperations = mock(SetOperations.class);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        return setOperations;
    }

    private String cacheKey(UUID tenantId, UUID userId) {
        return "rbac:permissions:%s:%s".formatted(tenantId, userId);
    }
}
