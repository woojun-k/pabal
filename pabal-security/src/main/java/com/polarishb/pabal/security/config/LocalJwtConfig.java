package com.polarishb.pabal.security.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.List;

@Configuration
@RequiredArgsConstructor
@Slf4j
@Profile({"local", "test"})
public class LocalJwtConfig {

    private static final String HMAC_SHA_256 = "HmacSHA256";
    private static final String KNOWN_LOCAL_SECRET_PLACEHOLDER = "0123456789".repeat(3) + "01";
    private static final int LOCAL_SECRET_BYTES = 32;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final JwtSecurityProperties jwtProperties;

    @Bean
    SecretKey localJwtSecretKey(@Value("${pabal.security.jwt.local-secret:}") String secret) {
        if (secret == null || secret.isBlank() || KNOWN_LOCAL_SECRET_PLACEHOLDER.equals(secret)) {
            byte[] keyBytes = new byte[LOCAL_SECRET_BYTES];
            SECURE_RANDOM.nextBytes(keyBytes);
            log.warn("Generated an ephemeral local/test JWT secret because pabal.security.jwt.local-secret is unset or uses the documented example placeholder.");
            return new SecretKeySpec(keyBytes, HMAC_SHA_256);
        }

        return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA_256);
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey localJwtSecretKey) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withSecretKey(localJwtSecretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        OAuth2TokenValidator<Jwt> withDefault = JwtValidators.createDefault();
        OAuth2TokenValidator<Jwt> withAudience = JwtClaimValidators.audience(jwtProperties.audience());
        OAuth2TokenValidator<Jwt> withRequiredClaims = JwtClaimValidators.requiredClaims(
                List.of(jwtProperties.userIdClaim(), jwtProperties.tenantIdClaim(), jwtProperties.principalClaim())
        );

        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(withDefault, withAudience, withRequiredClaims));
        return decoder;
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey localJwtSecretKey) {
        OctetSequenceKey jwk = new OctetSequenceKey.Builder(localJwtSecretKey)
                .keyID("local-hs256")
                .build();

        JWKSource<SecurityContext> jwkSource = new ImmutableJWKSet<>(new JWKSet(jwk));
        return new NimbusJwtEncoder(jwkSource);
    }
}
