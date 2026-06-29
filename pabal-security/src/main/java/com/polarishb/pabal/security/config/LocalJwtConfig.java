package com.polarishb.pabal.security.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import lombok.RequiredArgsConstructor;
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
import java.util.List;

@Configuration
@RequiredArgsConstructor
@Profile({"local", "test"})
public class LocalJwtConfig {

    private final JwtSecurityProperties jwtProperties;

    @Bean
    JwtDecoder jwtDecoder(@Value("${pabal.security.jwt.local-secret}") String secret) {
        SecretKey key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");

        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withSecretKey(key)
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
    JwtEncoder jwtEncoder(@Value("${pabal.security.jwt.local-secret}") String secret) {
        SecretKey key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");

        OctetSequenceKey jwk = new OctetSequenceKey.Builder(key)
                .keyID("local-hs256")
                .build();

        JWKSource<SecurityContext> jwkSource = new ImmutableJWKSet<>(new JWKSet(jwk));
        return new NimbusJwtEncoder(jwkSource);
    }
}
