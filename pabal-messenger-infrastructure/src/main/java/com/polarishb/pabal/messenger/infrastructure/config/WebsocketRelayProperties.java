package com.polarishb.pabal.messenger.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pabal.websocket.relay")
public record WebsocketRelayProperties(
    boolean enabled,
    String host,
    int port,
    String clientLogin,
    String clientPasscode,
    String systemLogin,
    String systemPasscode,
    String virtualHost,
    long systemHeartbeatSendInterval,
    long systemHeartbeatReceiveInterval
) {}