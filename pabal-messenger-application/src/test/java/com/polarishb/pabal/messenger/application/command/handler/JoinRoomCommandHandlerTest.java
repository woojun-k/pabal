package com.polarishb.pabal.messenger.application.command.handler;

import com.polarishb.pabal.common.event.DomainEventPublisher;
import com.polarishb.pabal.messenger.application.command.input.JoinRoomCommand;
import com.polarishb.pabal.messenger.application.port.out.persistence.ChatRoomMemberRepository;
import com.polarishb.pabal.messenger.application.port.out.persistence.ChatRoomRepository;
import com.polarishb.pabal.messenger.application.port.out.time.ClockPort;
import com.polarishb.pabal.messenger.application.service.ChatRoomAccessSupport;
import com.polarishb.pabal.messenger.contract.persistence.chatroom.ChatRoomState;
import com.polarishb.pabal.messenger.contract.persistence.chatroom.PersistedChatRoom;
import com.polarishb.pabal.messenger.contract.persistence.chatroommember.PersistedChatRoomMember;
import com.polarishb.pabal.messenger.domain.event.MemberJoinedEvent;
import com.polarishb.pabal.messenger.domain.exception.RoomJoinForbiddenException;
import com.polarishb.pabal.messenger.domain.model.ChatRoom;
import com.polarishb.pabal.messenger.domain.model.type.RoomStatus;
import com.polarishb.pabal.messenger.domain.model.type.RoomType;
import com.polarishb.pabal.messenger.domain.model.vo.ChannelName;
import com.polarishb.pabal.messenger.domain.model.vo.ChannelSettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JoinRoomCommandHandlerTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatRoomMemberRepository chatRoomMemberRepository;

    @Mock
    private DomainEventPublisher eventPublisher;

    @Mock
    private ClockPort clockPort;

    private JoinRoomCommandHandler handler;

    @BeforeEach
    void setUp() {
        ChatRoomAccessSupport chatRoomAccessSupport = new ChatRoomAccessSupport(
                chatRoomRepository,
                chatRoomMemberRepository
        );
        handler = new JoinRoomCommandHandler(
                chatRoomMemberRepository,
                chatRoomAccessSupport,
                eventPublisher,
                clockPort
        );
    }

    @Test
    void handle_appends_member_for_public_channel_join() {
        UUID tenantId = UUID.randomUUID();
        UUID chatRoomId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant now = Instant.parse("2026-04-02T12:00:00Z");
        PersistedChatRoom room = persistedChannel(tenantId, chatRoomId, false, 7L, now);

        when(chatRoomRepository.findByTenantIdAndId(tenantId, chatRoomId))
                .thenReturn(Optional.of(room));
        when(clockPort.now()).thenReturn(now);
        when(chatRoomMemberRepository.findByTenantIdAndChatRoomIdAndUserId(tenantId, chatRoomId, userId))
                .thenReturn(Optional.empty());
        when(chatRoomMemberRepository.append(any(PersistedChatRoomMember.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        handler.handle(new JoinRoomCommand(tenantId, chatRoomId, userId));

        ArgumentCaptor<PersistedChatRoomMember> memberCaptor = ArgumentCaptor.forClass(PersistedChatRoomMember.class);
        verify(chatRoomMemberRepository).append(memberCaptor.capture());
        assertThat(memberCaptor.getValue().member().getTenantId()).isEqualTo(tenantId);
        assertThat(memberCaptor.getValue().member().getChatRoomId()).isEqualTo(chatRoomId);
        assertThat(memberCaptor.getValue().member().getUserId()).isEqualTo(userId);
        assertThat(memberCaptor.getValue().member().getLastReadSequence()).isEqualTo(7L);
        assertThat(memberCaptor.getValue().member().getJoinedAt()).isEqualTo(now);

        verify(eventPublisher).publishAfterCommit(new MemberJoinedEvent(tenantId, chatRoomId, userId, 7L, now, null));
    }

    @Test
    void handle_rejects_private_channel_join_before_membership_write() {
        UUID tenantId = UUID.randomUUID();
        UUID chatRoomId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-04-02T12:00:00Z");

        when(chatRoomRepository.findByTenantIdAndId(tenantId, chatRoomId))
                .thenReturn(Optional.of(persistedChannel(tenantId, chatRoomId, true, 0L, createdAt)));

        assertThatThrownBy(() -> handler.handle(new JoinRoomCommand(tenantId, chatRoomId, userId)))
                .isInstanceOf(RoomJoinForbiddenException.class);

        verify(clockPort, never()).now();
        verify(chatRoomMemberRepository, never()).findByTenantIdAndChatRoomIdAndUserId(any(), any(), any());
        verify(chatRoomMemberRepository, never()).append(any(PersistedChatRoomMember.class));
        verify(chatRoomMemberRepository, never()).update(any(PersistedChatRoomMember.class));
        verify(eventPublisher, never()).publishAfterCommit(any());
    }

    private static PersistedChatRoom persistedChannel(
            UUID tenantId,
            UUID chatRoomId,
            boolean isPrivate,
            long lastMessageSequence,
            Instant createdAt
    ) {
        ChatRoom room = ChatRoom.reconstitute(
                chatRoomId,
                RoomType.CHANNEL,
                new ChannelName(isPrivate ? "secret" : "general"),
                UUID.randomUUID(),
                tenantId,
                new ChannelSettings(UUID.randomUUID(), isPrivate, null),
                RoomStatus.ACTIVE,
                null,
                null,
                lastMessageSequence,
                null,
                createdAt,
                createdAt
        );
        ChatRoomState state = new ChatRoomState(
                chatRoomId,
                RoomType.CHANNEL,
                room.getName().valueOrNull(),
                room.getCreatedBy(),
                tenantId,
                room.getChannelSettings(),
                RoomStatus.ACTIVE,
                null,
                null,
                lastMessageSequence,
                null,
                createdAt,
                createdAt,
                0L
        );
        return new PersistedChatRoom(room, state);
    }
}
