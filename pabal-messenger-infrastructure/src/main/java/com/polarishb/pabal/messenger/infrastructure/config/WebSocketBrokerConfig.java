package com.polarishb.pabal.messenger.infrastructure.config;

import com.polarishb.pabal.security.config.WebsocketEndpointProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketBrokerConfig implements WebSocketMessageBrokerConfigurer {

    private final WebsocketEndpointProperties endpointProperties;
    private final WebsocketRelayProperties relayProperties;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        String path = normalizePath(endpointProperties.path());

        var registration = registry
                .addEndpoint(path)
                .setAllowedOriginPatterns(endpointProperties.allowedOriginPatterns().toArray(String[]::new));

        if (endpointProperties.sockJsEnabled()) {
            registration.withSockJS();
        }
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/app");

        if (relayProperties.enabled()) {
            registry.enableStompBrokerRelay("/topic", "/queue")
                    .setRelayHost(relayProperties.host())
                    .setRelayPort(relayProperties.port())
                    .setClientLogin(relayProperties.clientLogin())
                    .setClientPasscode(relayProperties.clientPasscode())
                    .setSystemLogin(relayProperties.systemLogin())
                    .setSystemPasscode(relayProperties.systemPasscode())
                    .setVirtualHost(relayProperties.virtualHost())
                    .setSystemHeartbeatSendInterval(relayProperties.systemHeartbeatSendInterval())
                    .setSystemHeartbeatReceiveInterval(relayProperties.systemHeartbeatReceiveInterval());
        } else {
            registry.enableSimpleBroker("/topic", "/queue");
        }
    }

    private String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return "/websocket";
        }
        return path.endsWith("/") && path.length() > 1
                ? path.substring(0, path.length() - 1)
                : path;
    }
}