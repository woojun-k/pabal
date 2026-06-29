package com.polarishb.pabal.security.dev;

import com.polarishb.pabal.security.config.JwtSecurityProperties;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class LocalDevTokenControllerTest {

    private final JwtSecurityProperties properties = new JwtSecurityProperties(
            "local-dev",
            "pabal-api",
            "uid",
            "tenant_id",
            "sub",
            Duration.ofSeconds(30),
            Duration.ofMinutes(60),
            Duration.ofMinutes(90),
            Duration.ofDays(7)
    );

    @Test
    void token_issues_access_token_without_persisting_refresh_token() {
        FakeJwtEncoder encoder = new FakeJwtEncoder();
        LocalDevTokenController controller = new LocalDevTokenController(properties, encoder);
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();

        Map<String, String> response = controller.token(
                userId,
                tenantId,
                List.of("messenger:channel:create"),
                List.of("tenant_admin"),
                List.of("messenger:channel:delete:any")
        );

        assertThat(response)
                .containsEntry("tokenType", "Bearer")
                .containsEntry("accessToken", "access-token")
                .containsEntry("audience", "pabal-api")
                .containsKey("accessTokenExpiresAt")
                .doesNotContainKeys("refreshToken", "refreshTokenExpiresAt");
        assertThat(encoder.lastClaims())
                .containsEntry("uid", userId.toString())
                .containsEntry("tenant_id", tenantId.toString())
                .containsEntry("scope", "messenger:channel:create")
                .containsEntry("roles", List.of("tenant_admin"))
                .containsEntry("permissions", List.of("messenger:channel:delete:any"));
    }

    private static final class FakeJwtEncoder implements JwtEncoder {
        private Map<String, Object> lastClaims = Map.of();

        @Override
        public Jwt encode(JwtEncoderParameters parameters) {
            lastClaims = new HashMap<>(parameters.getClaims().getClaims());
            return new Jwt(
                    "access-token",
                    parameters.getClaims().getIssuedAt(),
                    parameters.getClaims().getExpiresAt(),
                    Map.of("alg", "HS256"),
                    lastClaims
            );
        }

        private Map<String, Object> lastClaims() {
            return lastClaims;
        }
    }
}
