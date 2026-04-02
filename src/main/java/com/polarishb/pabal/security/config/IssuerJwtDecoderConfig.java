package com.polarishb.pabal.security.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.*;

import java.util.List;

@Configuration
@RequiredArgsConstructor
@Profile("!local & !test")
public class IssuerJwtDecoderConfig {

    private final JwtSecurityProperties jwtProperties;

    @Bean
    JwtDecoder jwtDecoder() {
        NimbusJwtDecoder decoder = JwtDecoders.fromIssuerLocation(jwtProperties.issuerUri()) instanceof NimbusJwtDecoder nimbus
                ? nimbus
                : NimbusJwtDecoder.withIssuerLocation(jwtProperties.issuerUri()).build();

        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(jwtProperties.issuerUri());
        OAuth2TokenValidator<Jwt> withAudience = audienceValidator(jwtProperties.audience());
        OAuth2TokenValidator<Jwt> withRequiredClaims = requiredClaimsValidator(
                List.of(jwtProperties.userIdClaim(), jwtProperties.tenantIdClaim(), jwtProperties.principalClaim())
        );

        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(withIssuer, withAudience, withRequiredClaims));
        return decoder;
    }

    private OAuth2TokenValidator<Jwt> audienceValidator(String requiredAudience) {
        return jwt -> jwt.getAudience().contains(requiredAudience)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(
                new OAuth2Error("invalid_token", "Missing required audience", null)
        );
    }

    private OAuth2TokenValidator<Jwt> requiredClaimsValidator(List<String> claims) {
        return jwt -> claims.stream().allMatch(jwt.getClaims()::containsKey)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(
                new OAuth2Error("invalid_token", "Missing required claims", null)
        );
    }

}
