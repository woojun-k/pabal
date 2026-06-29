package com.polarishb.pabal.security.config;

import com.polarishb.pabal.security.authentication.PabalJwtAuthenticationConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Arrays;
import java.util.Set;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties({
        JwtSecurityProperties.class,
        RefreshTokenReplayProperties.class,
        WebsocketEndpointProperties.class
})
public class SecurityConfig {

    private static final Set<String> DEV_TOKEN_PROFILES = Set.of("local", "test");

    private final WebsocketEndpointProperties websocketEndpointProperties;
    private final PabalJwtAuthenticationConverter jwtAuthenticationConverter;
    private final Environment environment;

    @Bean
    SecurityFilterChain apiSecurityFilterChain(HttpSecurity http, JwtDecoder jwtDecoder) throws Exception {
        String wsPath = normalizePath(websocketEndpointProperties.path());

        http
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers("/actuator/health").permitAll()
                            .requestMatchers(wsPath, wsPath + "/**").permitAll()
                            .requestMatchers(
                                    "/api/v1/auth/tokens/refresh",
                                    "/api/v1/auth/tokens/revoke",
                                    "/api/v1/tenant-registrations",
                                    "/api/v1/tenant-registrations/**"
                            ).permitAll();

                    if (isDevTokenProfileActive()) {
                        auth.requestMatchers("/dev/**").permitAll();
                    }

                    auth.anyRequest().authenticated();
                })
                .sessionManagement(session -> session.sessionCreationPolicy(
                        SessionCreationPolicy.STATELESS
                ))
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .decoder(jwtDecoder)
                                .jwtAuthenticationConverter(jwtAuthenticationConverter)
                        )
                )
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(form -> form.disable())
                .csrf(csrf -> csrf.disable());

        return http.build();
    }

    private String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return "/websocket";
        }
        return path.endsWith("/") && path.length() > 1
                ? path.substring(0, path.length() - 1)
                : path;
    }

    private boolean isDevTokenProfileActive() {
        return Arrays.stream(environment.getActiveProfiles())
                .anyMatch(DEV_TOKEN_PROFILES::contains);
    }
}
