package com.polarishb.pabal.messenger.infrastructure.realtime.ws.security;

import com.polarishb.pabal.security.authentication.PabalJwtAuthenticationConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;

@Configuration
public class WebSocketAuthenticationManagerConfig {

    @Bean
    AuthenticationManager websocketAuthenticationManager(
            JwtDecoder jwtDecoder,
            PabalJwtAuthenticationConverter jwtAuthenticationConverter
    ) {
        JwtAuthenticationProvider provider = new JwtAuthenticationProvider(jwtDecoder);
        provider.setJwtAuthenticationConverter(jwtAuthenticationConverter);
        return new ProviderManager(provider);
    }
}
