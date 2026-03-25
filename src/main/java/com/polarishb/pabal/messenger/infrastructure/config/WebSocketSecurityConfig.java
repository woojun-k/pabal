package com.polarishb.pabal.messenger.infrastructure.config;

import com.polarishb.pabal.messenger.infrastructure.realtime.ws.security.RoomSubscriptionAuthorizationManager;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.web.socket.EnableWebSocketSecurity;
import org.springframework.security.messaging.access.intercept.MessageMatcherDelegatingAuthorizationManager;

@Configuration
@EnableWebSocketSecurity
@RequiredArgsConstructor
public class WebSocketSecurityConfig {

    private final RoomSubscriptionAuthorizationManager roomSubscriptionAuthorizationManager;

    @Bean
    AuthorizationManager<Message<?>> messageAuthorizationManager(
            MessageMatcherDelegatingAuthorizationManager.Builder messages
    ) {
        messages
                .nullDestMatcher().authenticated()
                .simpDestMatchers("/app/**").authenticated()
                .simpSubscribeDestMatchers(
                        "/topic/tenants/{tenantId}/chat-rooms/{chatRoomId}/events",
                        "/topic/tenants/{tenantId}/chat-rooms/{chatRoomId}/typing"
                ).access(roomSubscriptionAuthorizationManager)
                .simpTypeMatchers(SimpMessageType.MESSAGE, SimpMessageType.SUBSCRIBE).denyAll()
                .anyMessage().denyAll();

        return messages.build();
    }
}
