package com.polarishb.pabal.security.authentication;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PabalJwtAuthenticationTokenTest {

    @Test
    void getDestinationUserName_uses_tenant_and_user_id() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        PabalPrincipal principal = new PabalPrincipal(userId, tenantId, "subject");
        Jwt jwt = new Jwt(
                "token",
                Instant.parse("2026-04-08T00:00:00Z"),
                Instant.parse("2026-04-08T01:00:00Z"),
                Map.of("alg", "none"),
                Map.of("sub", "subject")
        );

        PabalJwtAuthenticationToken authenticationToken =
                new PabalJwtAuthenticationToken(principal, jwt, List.of());

        assertThat(authenticationToken.getDestinationUserName())
                .isEqualTo(PabalPrincipal.destinationUserName(tenantId, userId));
    }
}
