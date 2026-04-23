package com.polarishb.pabal.messenger.infrastructure.realtime.ws.security;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class StompConnectAuthenticationInterceptor implements ChannelInterceptor {

    private final AuthenticationManager websocketAuthenticationManager;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) {
            return message;
        }

        if (!StompCommand.CONNECT.equals(accessor.getCommand())) {
            return message;
        }

        String token = resolveAccessToken(accessor);
        Authentication authentication = websocketAuthenticationManager.authenticate(
                new BearerTokenAuthenticationToken(token)
        );

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BadCredentialsException("Invalid STOMP access token");
        }

        accessor.setUser(authentication);
        return message;
    }

    private String resolveAccessToken(StompHeaderAccessor accessor) {
        String authorization = accessor.getFirstNativeHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(authorization) && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }

        String accessToken = accessor.getFirstNativeHeader("access_token");
        if (StringUtils.hasText(accessToken)) {
            return accessToken;
        }

        throw new BadCredentialsException("Missing STOMP access token");
    }
}
