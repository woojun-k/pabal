package com.polarishb.pabal.messenger.infrastructure.realtime.ws.security;

import com.polarishb.pabal.messenger.application.port.out.persistence.ChatRoomMemberReadRepository;
import com.polarishb.pabal.messenger.application.port.out.persistence.ChatRoomReadRepository;
import com.polarishb.pabal.messenger.contract.persistence.chatroom.ChatRoomState;
import com.polarishb.pabal.messenger.contract.persistence.chatroommember.ChatRoomMemberState;
import com.polarishb.pabal.messenger.domain.model.type.RoomStatus;
import com.polarishb.pabal.messenger.domain.model.type.RoomType;
import com.polarishb.pabal.security.authentication.PabalPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.messaging.access.intercept.MessageAuthorizationContext;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class RoomSubscriptionAuthorizationManagerTest {

    private static final Instant NOW = Instant.parse("2026-06-19T00:00:00Z");

    private final ChatRoomReadRepository chatRoomReadRepository = mock(ChatRoomReadRepository.class);
    private final ChatRoomMemberReadRepository chatRoomMemberReadRepository = mock(ChatRoomMemberReadRepository.class);

    private RoomSubscriptionAuthorizationManager manager;

    @BeforeEach
    void setUp() {
        manager = new RoomSubscriptionAuthorizationManager(
                chatRoomReadRepository,
                chatRoomMemberReadRepository
        );
    }

    @Test
    void authorize_allows_active_member_to_subscribe_to_active_room_topic() {
        UUID tenantId = UUID.randomUUID();
        UUID chatRoomId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(chatRoomReadRepository.findByTenantIdAndId(tenantId, chatRoomId))
                .thenReturn(Optional.of(room(tenantId, chatRoomId, RoomStatus.ACTIVE)));
        when(chatRoomMemberReadRepository.findByTenantIdAndChatRoomIdAndUserId(tenantId, chatRoomId, userId))
                .thenReturn(Optional.of(member(tenantId, chatRoomId, userId, true)));

        AuthorizationResult result = authorize(
                authentication(tenantId, userId),
                roomEventsTopic(tenantId, chatRoomId)
        );

        assertThat(result.isGranted()).isTrue();
    }

    @Test
    void authorize_denies_non_member_subscribe_even_when_room_is_active() {
        UUID tenantId = UUID.randomUUID();
        UUID chatRoomId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(chatRoomReadRepository.findByTenantIdAndId(tenantId, chatRoomId))
                .thenReturn(Optional.of(room(tenantId, chatRoomId, RoomStatus.ACTIVE)));
        when(chatRoomMemberReadRepository.findByTenantIdAndChatRoomIdAndUserId(tenantId, chatRoomId, userId))
                .thenReturn(Optional.empty());

        AuthorizationResult result = authorize(
                authentication(tenantId, userId),
                roomEventsTopic(tenantId, chatRoomId)
        );

        assertThat(result.isGranted()).isFalse();
        verify(chatRoomMemberReadRepository).findByTenantIdAndChatRoomIdAndUserId(tenantId, chatRoomId, userId);
    }

    @Test
    void authorize_denies_room_when_canSubscribe_is_false_before_member_lookup() {
        UUID tenantId = UUID.randomUUID();
        UUID chatRoomId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(chatRoomReadRepository.findByTenantIdAndId(tenantId, chatRoomId))
                .thenReturn(Optional.of(room(tenantId, chatRoomId, RoomStatus.PENDING_DELETION)));

        AuthorizationResult result = authorize(
                authentication(tenantId, userId),
                roomEventsTopic(tenantId, chatRoomId)
        );

        assertThat(result.isGranted()).isFalse();
        verifyNoInteractions(chatRoomMemberReadRepository);
    }

    @Test
    void authorize_denies_cross_tenant_subscription_before_repository_lookup() {
        UUID principalTenantId = UUID.randomUUID();
        UUID destinationTenantId = UUID.randomUUID();

        AuthorizationResult result = authorize(
                authentication(principalTenantId, UUID.randomUUID()),
                roomEventsTopic(destinationTenantId, UUID.randomUUID())
        );

        assertThat(result.isGranted()).isFalse();
        verifyNoInteractions(chatRoomReadRepository, chatRoomMemberReadRepository);
    }

    @Test
    void authorize_denies_unknown_room_before_member_lookup() {
        UUID tenantId = UUID.randomUUID();
        UUID chatRoomId = UUID.randomUUID();
        when(chatRoomReadRepository.findByTenantIdAndId(tenantId, chatRoomId))
                .thenReturn(Optional.empty());

        AuthorizationResult result = authorize(
                authentication(tenantId, UUID.randomUUID()),
                roomEventsTopic(tenantId, chatRoomId)
        );

        assertThat(result.isGranted()).isFalse();
        verifyNoInteractions(chatRoomMemberReadRepository);
    }

    private AuthorizationResult authorize(Authentication authentication, String destination) {
        return manager.authorize(
                () -> authentication,
                new MessageAuthorizationContext<>(subscribeMessage(destination))
        );
    }

    private Message<?> subscribeMessage(String destination) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(SimpMessageType.SUBSCRIBE);
        accessor.setDestination(destination);
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private Authentication authentication(UUID tenantId, UUID userId) {
        return new UsernamePasswordAuthenticationToken(
                new PabalPrincipal(userId, tenantId, "subject"),
                "n/a",
                List.of()
        );
    }

    private ChatRoomState room(UUID tenantId, UUID chatRoomId, RoomStatus status) {
        return new ChatRoomState(
                chatRoomId,
                RoomType.GROUP,
                "Room",
                UUID.randomUUID(),
                tenantId,
                null,
                status,
                status == RoomStatus.PENDING_DELETION ? NOW.plusSeconds(60) : null,
                null,
                0L,
                null,
                NOW,
                NOW,
                0L
        );
    }

    private ChatRoomMemberState member(
            UUID tenantId,
            UUID chatRoomId,
            UUID userId,
            boolean active
    ) {
        return new ChatRoomMemberState(
                UUID.randomUUID(),
                tenantId,
                chatRoomId,
                userId,
                null,
                0L,
                null,
                NOW,
                active ? null : NOW.plusSeconds(1),
                NOW,
                NOW,
                0L
        );
    }

    private String roomEventsTopic(UUID tenantId, UUID chatRoomId) {
        return "/topic/tenants/" + tenantId + "/chat-rooms/" + chatRoomId + "/events";
    }
}
