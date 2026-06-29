package com.polarishb.pabal.messenger.infrastructure.realtime.ws.security;

import com.polarishb.pabal.security.authentication.PabalPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class StompConnectAuthenticationInterceptorTest {

    private final AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
    private final MessageChannel channel = mock(MessageChannel.class);

    private StompConnectAuthenticationInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new StompConnectAuthenticationInterceptor(authenticationManager);
    }

    @Test
    void preSend_authenticates_connect_with_authorization_bearer_header_and_sets_stomp_user() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        PabalPrincipal principal = new PabalPrincipal(userId, tenantId, "subject");
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                principal,
                "access-token",
                List.of()
        );
        when(authenticationManager.authenticate(argThat(token ->
                token instanceof BearerTokenAuthenticationToken bearer
                        && bearer.getToken().equals("access-token")
        ))).thenReturn(authentication);

        Message<?> message = connectMessage(HttpHeaders.AUTHORIZATION, "Bearer access-token");

        Message<?> result = interceptor.preSend(message, channel);

        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(result);
        assertThat(accessor.getUser()).isInstanceOf(StompAuthenticationToken.class);
        assertThat(((StompAuthenticationToken) accessor.getUser()).getPrincipal()).isSameAs(principal);
    }

    @Test
    void preSend_rejects_connect_without_token_before_authentication_manager_call() {
        Message<?> message = connectMessage(null, null);

        assertThatThrownBy(() -> interceptor.preSend(message, channel))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Missing STOMP access token");

        verifyNoInteractions(authenticationManager);
    }

    @Test
    void preSend_rejects_connect_when_authentication_manager_reports_expired_token() {
        when(authenticationManager.authenticate(any(BearerTokenAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Jwt expired"));

        Message<?> message = connectMessage(HttpHeaders.AUTHORIZATION, "Bearer expired-token");

        assertThatThrownBy(() -> interceptor.preSend(message, channel))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Jwt expired");
    }

    @Test
    void preSend_ignores_non_connect_messages() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThat(interceptor.preSend(message, channel)).isSameAs(message);
        verifyNoInteractions(authenticationManager);
    }

    private Message<?> connectMessage(String headerName, String headerValue) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        if (headerName != null) {
            accessor.setNativeHeader(headerName, headerValue);
        }
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
