package com.polarishb.pabal.authorization;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class JdbcRbacPermissionStore implements RbacPermissionStore {

    private static final Duration CACHE_TTL = Duration.ofMinutes(5);
    private static final String CACHE_MARKER = "__pabal_rbac_permissions_cached__";

    private static final String FIND_PERMISSION_VALUES = """
            SELECT DISTINCT p.value
            FROM rbac_user_role ur
            JOIN rbac_role r
              ON r.tenant_id = ur.tenant_id
             AND r.id = ur.role_id
            JOIN rbac_role_permission rp
              ON rp.tenant_id = r.tenant_id
             AND rp.role_id = r.id
            JOIN rbac_permission p
              ON p.id = rp.permission_id
            WHERE ur.tenant_id = :tenantId
              AND ur.user_id = :userId
              AND ur.revoked_at IS NULL
              AND r.status = 'ACTIVE'
            """;

    private final JdbcClient jdbcClient;
    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;

    @Autowired
    public JdbcRbacPermissionStore(
            ObjectProvider<DataSource> dataSourceProvider,
            ObjectProvider<StringRedisTemplate> redisTemplateProvider
    ) {
        this(createJdbcClient(dataSourceProvider), redisTemplateProvider);
    }

    JdbcRbacPermissionStore(
            JdbcClient jdbcClient,
            ObjectProvider<StringRedisTemplate> redisTemplateProvider
    ) {
        this.jdbcClient = jdbcClient;
        this.redisTemplateProvider = Objects.requireNonNull(redisTemplateProvider, "redisTemplateProvider must not be null");
    }

    private static JdbcClient createJdbcClient(ObjectProvider<DataSource> dataSourceProvider) {
        DataSource dataSource = dataSourceProvider.getIfAvailable();
        return dataSource == null ? null : JdbcClient.create(dataSource);
    }

    @Override
    public Set<String> findPermissionValues(UUID tenantId, UUID userId) {
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(userId, "userId must not be null");

        Set<String> cachedPermissions = findCachedPermissionValues(tenantId, userId);
        if (cachedPermissions != null) {
            return cachedPermissions;
        }

        if (jdbcClient == null) {
            return Set.of();
        }

        List<String> permissions = jdbcClient.sql(FIND_PERMISSION_VALUES)
                .param("tenantId", tenantId)
                .param("userId", userId)
                .query(String.class)
                .list();

        Set<String> permissionSet = Set.copyOf(permissions);
        cachePermissionValues(tenantId, userId, permissionSet);
        return permissionSet;
    }

    @Override
    public void evictPermissionValues(UUID tenantId, UUID userId) {
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(userId, "userId must not be null");

        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate == null) {
            return;
        }

        try {
            redisTemplate.delete(cacheKey(tenantId, userId));
        } catch (RuntimeException ignored) {
            // Permission eviction is best effort; the DB remains the source of truth.
        }
    }

    private Set<String> findCachedPermissionValues(UUID tenantId, UUID userId) {
        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate == null) {
            return null;
        }

        String cacheKey = cacheKey(tenantId, userId);
        try {
            if (!Boolean.TRUE.equals(redisTemplate.hasKey(cacheKey))) {
                return null;
            }
            Set<String> cachedValues = redisTemplate.opsForSet().members(cacheKey);
            if (cachedValues == null) {
                return null;
            }
            return cachedValues.stream()
                    .filter(value -> !CACHE_MARKER.equals(value))
                    .collect(Collectors.toUnmodifiableSet());
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private void cachePermissionValues(UUID tenantId, UUID userId, Set<String> permissionValues) {
        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate == null) {
            return;
        }

        String cacheKey = cacheKey(tenantId, userId);
        List<String> values = new ArrayList<>(permissionValues.size() + 1);
        values.add(CACHE_MARKER);
        values.addAll(permissionValues);

        try {
            redisTemplate.delete(cacheKey);
            redisTemplate.opsForSet().add(cacheKey, values.toArray(String[]::new));
            redisTemplate.expire(cacheKey, CACHE_TTL);
        } catch (RuntimeException ignored) {
            // Cache failures must not change authorization truth; fall back to DB on the next request.
        }
    }

    private String cacheKey(UUID tenantId, UUID userId) {
        return "rbac:permissions:%s:%s".formatted(tenantId, userId);
    }
}
