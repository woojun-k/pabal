package com.polarishb.pabal.security.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
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
        OAuth2TokenValidator<Jwt> withAudience = JwtClaimValidators.audience(jwtProperties.audience());
        OAuth2TokenValidator<Jwt> withRequiredClaims = JwtClaimValidators.requiredClaims(
                List.of(jwtProperties.userIdClaim(), jwtProperties.tenantIdClaim(), jwtProperties.principalClaim())
        );

        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(withIssuer, withAudience, withRequiredClaims));
        return decoder;
    }
}
