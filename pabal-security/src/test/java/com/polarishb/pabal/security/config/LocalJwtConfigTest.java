package com.polarishb.pabal.security.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LocalJwtConfigTest {

    private static final String CONFIGURED_SECRET = "abcdefghijklmnopqrstuvwxyz123456";
    private static final String KNOWN_PLACEHOLDER = "0123456789".repeat(3) + "01";

    private final LocalJwtConfig config = new LocalJwtConfig(new JwtSecurityProperties(
            "local-dev",
            "pabal-api",
            "uid",
            "tenant_id",
            "sub",
            null,
            null,
            null,
            null
    ));

    @Test
    void usesConfiguredSecretAsHmacKey() {
        SecretKey key = config.localJwtSecretKey(CONFIGURED_SECRET);

        assertThat(key.getEncoded()).isEqualTo(CONFIGURED_SECRET.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void generatesEphemeralSecretWhenSecretIsBlank() {
        SecretKey key = config.localJwtSecretKey("");

        assertThat(key.getEncoded()).hasSize(32);
    }

    @Test
    void generatesEphemeralSecretWhenSecretUsesKnownPlaceholder() {
        SecretKey key = config.localJwtSecretKey(KNOWN_PLACEHOLDER);

        assertThat(key.getEncoded())
                .hasSize(32)
                .isNotEqualTo(KNOWN_PLACEHOLDER.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void generatedSecretCanEncodeAndDecodeLocalTokenInSameContext() {
        SecretKey key = config.localJwtSecretKey("");
        JwtEncoder encoder = config.jwtEncoder(key);
        JwtDecoder decoder = config.jwtDecoder(key);
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("local-dev")
                .subject("local-subject")
                .audience(List.of("pabal-api"))
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300))
                .claim("uid", "018f0000-0000-7000-8000-000000000001")
                .claim("tenant_id", "018f0000-0000-7000-8000-000000000100")
                .build();

        Jwt encoded = encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(),
                claims
        ));
        Jwt decoded = decoder.decode(encoded.getTokenValue());

        assertThat(decoded.getSubject()).isEqualTo("local-subject");
    }
}
