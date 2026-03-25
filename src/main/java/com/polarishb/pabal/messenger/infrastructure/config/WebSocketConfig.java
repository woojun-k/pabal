package com.polarishb.pabal.messenger.infrastructure.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.StompWebSocketEndpointRegistration;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@RequiredArgsConstructor
@EnableWebSocketMessageBroker
@EnableConfigurationProperties({
        WebsocketRelayProperties.class,
        WebsocketEndpointProperties.class
})
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebsocketRelayProperties relayProperties;
    private final WebsocketEndpointProperties endpointProperties;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
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

        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        StompWebSocketEndpointRegistration endpoint = registry.addEndpoint(endpointProperties.path())
                .setAllowedOriginPatterns(
                        endpointProperties.allowedOriginPatterns().toArray(String[]::new)
                );

        if (endpointProperties.sockJsEnabled()) {
            endpoint.withSockJS();
        }
    }
}
