package com.polarishb.pabal.security.config;

import com.polarishb.pabal.security.authentication.PabalJwtAuthenticationConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(JwtSecurityProperties.class)
public class SecurityConfig {

    private final JwtSecurityProperties jwtProperties;
    private final PabalJwtAuthenticationConverter jwtAuthenticationConverter;

    @Bean
    SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/websocket/**").permitAll()
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(
                        SessionCreationPolicy.STATELESS
                ))
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .decoder(jwtDecoder())
                                .jwtAuthenticationConverter(jwtAuthenticationConverter)
                        )
                )
                .httpBasic(Customizer.withDefaults())
                .formLogin(form -> form.disable())
                .csrf(csrf -> csrf.disable());

        return http.build();
    }

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
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Missing required audience", null));
    }

    private OAuth2TokenValidator<Jwt> requiredClaimsValidator(List<String> claims) {
        return jwt -> claims.stream().allMatch(jwt.getClaims()::containsKey)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Missing required claims", null));
    }
}
