package com.polarishb.pabal.messenger.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.socket.EnableWebSocketSecurity;

@Configuration
@EnableWebSocketSecurity
@Profile("!local & !test")
public class WebSocketSecurityConfig {
}
