package com.polarishb.pabal.messenger.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "pabal.websocket.endpoint")
public record WebsocketEndpointProperties(
    String path,
    List<String> allowedOriginPatterns,
    boolean sockJsEnabled
) {}