package com.polarishb.pabal.security.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JwtClaimValidatorsTest {

    @Test
    void requiredClaims_rejects_claim_with_null_value() {
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("sub", "subject");
        claims.put("uid", null);
        Jwt jwt = jwt(claims);
        OAuth2TokenValidator<Jwt> validator = JwtClaimValidators.requiredClaims(List.of("uid"));

        assertThat(validator.validate(jwt).getErrors())
                .extracting("description")
                .contains("Missing required claims");
    }

    @Test
    void requiredClaims_accepts_present_non_null_claims() {
        Jwt jwt = jwt(Map.of("sub", "subject", "uid", "user-id"));
        OAuth2TokenValidator<Jwt> validator = JwtClaimValidators.requiredClaims(List.of("uid"));

        assertThat(validator.validate(jwt).getErrors()).isEmpty();
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
