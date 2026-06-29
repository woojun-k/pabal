package com.polarishb.pabal.security.authentication;

import com.polarishb.pabal.authorization.AuthorityNormalizer;
import com.polarishb.pabal.security.config.JwtSecurityProperties;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PabalJwtAuthenticationConverterTest {

    private final JwtSecurityProperties properties = new JwtSecurityProperties(
            "issuer",
            "pabal-api",
            "uid",
            "tenant_id",
            "sub",
            Duration.ofSeconds(30),
            Duration.ofMinutes(60),
            Duration.ofMinutes(90),
            Duration.ofDays(7)
    );

    private final PabalJwtAuthenticationConverter converter = new PabalJwtAuthenticationConverter(properties);

    @Test
    void convert_maps_scope_permissions_and_roles_to_authorities() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        Jwt jwt = jwt(userId, tenantId, Map.of(
                "scope", "messenger:channel:create",
                "permissions", List.of("messenger:channel:delete:execute:any"),
                "roles", List.of("tenant-admin")
        ));

        AbstractAuthenticationToken authentication = converter.convert(jwt);

        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .contains(
                        "SCOPE_messenger:channel:create",
                        "messenger:channel:delete:execute:any",
                        "ROLE_TENANT_ADMIN"
                );
    }

    @Test
    void convert_maps_keycloak_realm_and_resource_roles_to_authorities() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        Jwt jwt = jwt(userId, tenantId, Map.of(
                "realm_access", Map.of("roles", List.of("workspace-admin")),
                "resource_access", Map.of(
                        "pabal-api",
                        Map.of("roles", List.of("pabal-admin"))
                )
        ));

        AbstractAuthenticationToken authentication = converter.convert(jwt);

        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .contains("ROLE_WORKSPACE_ADMIN", "ROLE_PABAL_ADMIN");
    }

    @Test
    void convert_normalizes_scoped_roles_for_rbac_matching() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        Jwt jwt = jwt(userId, tenantId, Map.of(
                "roles", List.of(
                        "tenant:%s:admin".formatted(tenantId),
                        "workspace:%s:owner".formatted(workspaceId)
                )
        ));

        AbstractAuthenticationToken authentication = converter.convert(jwt);

        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .contains(
                        AuthorityNormalizer.role("tenant:%s:admin".formatted(tenantId)),
                        AuthorityNormalizer.role("workspace:%s:owner".formatted(workspaceId))
                );
    }

    @Test
    void convert_throws_when_required_claim_is_missing() {
        Map<String, Object> claims = claims(UUID.randomUUID(), UUID.randomUUID());
        claims.remove("uid");
        Jwt jwt = jwt(claims);

        assertThatThrownBy(() -> converter.convert(jwt))
                .isInstanceOfSatisfying(OAuth2AuthenticationException.class, ex ->
                        assertThat(ex.getError().getDescription())
                                .contains("Required JWT claim 'uid' is missing")
                );
    }

    @Test
    void convert_throws_when_uid_claim_is_not_a_valid_uuid() {
        Map<String, Object> claims = claims(UUID.randomUUID(), UUID.randomUUID());
        claims.put("uid", "not-a-uuid");
        Jwt jwt = jwt(claims);

        assertThatThrownBy(() -> converter.convert(jwt))
                .isInstanceOfSatisfying(OAuth2AuthenticationException.class, ex ->
                        assertThat(ex.getError().getDescription())
                                .contains("JWT claim 'uid' is not a valid UUID")
                );
    }

    private Jwt jwt(UUID userId, UUID tenantId, Map<String, Object> extraClaims) {
        Map<String, Object> claims = claims(userId, tenantId);
        claims.putAll(extraClaims);

        return jwt(claims);
    }

    private Map<String, Object> claims(UUID userId, UUID tenantId) {
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("sub", userId.toString());
        claims.put("aud", List.of("pabal-api"));
        claims.put("uid", userId.toString());
        claims.put("tenant_id", tenantId.toString());
        return claims;
    }

    private Jwt jwt(Map<String, Object> claims) {
        return new Jwt(
                "token",
                Instant.parse("2026-04-08T00:00:00Z"),
                Instant.parse("2026-04-08T01:00:00Z"),
                Map.of("alg", "none"),
                claims
        );
    }
}
