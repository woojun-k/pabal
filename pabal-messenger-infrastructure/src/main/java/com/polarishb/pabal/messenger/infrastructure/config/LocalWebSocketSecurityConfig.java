package com.polarishb.pabal.messenger.infrastructure.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.messaging.access.intercept.AuthorizationChannelInterceptor;
import org.springframework.security.authorization.SpringAuthorizationEventPublisher;
import org.springframework.security.messaging.context.SecurityContextChannelInterceptor;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@Profile({"local", "test"})
@RequiredArgsConstructor
public class LocalWebSocketSecurityConfig implements WebSocketMessageBrokerConfigurer {

    private final ApplicationContext applicationContext;
    private final AuthorizationManager<Message<?>> messageAuthorizationManager;

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        AuthorizationChannelInterceptor authorization =
                new AuthorizationChannelInterceptor(messageAuthorizationManager);

        authorization.setAuthorizationEventPublisher(
                new SpringAuthorizationEventPublisher(applicationContext)
        );

        registration.interceptors(
                new SecurityContextChannelInterceptor(),
                authorization
        );
    }
}