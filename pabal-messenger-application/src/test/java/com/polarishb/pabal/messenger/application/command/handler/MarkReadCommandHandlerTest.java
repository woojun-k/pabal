package com.polarishb.pabal.messenger.application.command.handler;

import com.polarishb.pabal.common.event.DomainEventPublisher;
import com.polarishb.pabal.messenger.application.command.input.MarkReadCommand;
import com.polarishb.pabal.messenger.application.port.out.time.ClockPort;
import com.polarishb.pabal.messenger.application.service.ChatRoomAccessSupport;
import com.polarishb.pabal.messenger.application.service.context.ChatRoomAccess;
import com.polarishb.pabal.messenger.contract.persistence.chatroom.ChatRoomState;
import com.polarishb.pabal.messenger.contract.persistence.chatroom.PersistedChatRoom;
import com.polarishb.pabal.messenger.contract.persistence.chatroommember.ChatRoomMemberState;
import com.polarishb.pabal.messenger.contract.persistence.chatroommember.PersistedChatRoomMember;
import com.polarishb.pabal.messenger.contract.persistence.message.MessageState;
import com.polarishb.pabal.messenger.contract.persistence.message.PersistedMessage;
import com.polarishb.pabal.messenger.domain.event.MessageReadEvent;
import com.polarishb.pabal.messenger.domain.model.ChatRoom;
import com.polarishb.pabal.messenger.domain.model.ChatRoomMember;
import com.polarishb.pabal.messenger.domain.model.Message;
import com.polarishb.pabal.messenger.domain.model.type.MessageStatus;
import com.polarishb.pabal.messenger.domain.model.type.MessageType;
import com.polarishb.pabal.messenger.domain.model.type.RoomStatus;
import com.polarishb.pabal.messenger.domain.model.type.RoomType;
import com.polarishb.pabal.messenger.domain.model.vo.OptionalName;
import com.polarishb.pabal.messenger.application.port.out.persistence.ChatRoomMemberRepository;
import com.polarishb.pabal.messenger.application.port.out.persistence.MessageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarkReadCommandHandlerTest {

    @Mock
    private ChatRoomMemberRepository chatRoomMemberRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ChatRoomAccessSupport chatRoomAccessSupport;

    @Mock
    private DomainEventPublisher eventPublisher;

    @Mock
    private ClockPort clockPort;

    @InjectMocks
    private MarkReadCommandHandler handler;

    @Test
    void handle_publishes_read_event_when_cursor_advances() {
        UUID tenantId = UUID.randomUUID();
        UUID chatRoomId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID currentMessageId = UUID.randomUUID();
        UUID lastReadMessageId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-04-02T00:00:00Z");
        Instant readAt = createdAt.plusSeconds(30);

        PersistedChatRoom room = persistedRoom(tenantId, chatRoomId, userId, createdAt);
        PersistedChatRoomMember member = persistedMember(
                tenantId,
                chatRoomId,
                userId,
                currentMessageId,
                8L,
                createdAt.plusSeconds(10),
                createdAt
        );
        PersistedMessage lastReadMessage = persistedMessage(tenantId, chatRoomId, userId, lastReadMessageId, 9L, createdAt);
        MarkReadCommand command = new MarkReadCommand(tenantId, chatRoomId, userId, lastReadMessageId);

        when(chatRoomAccessSupport.loadReadableActiveMember(tenantId, chatRoomId, userId))
                .thenReturn(new ChatRoomAccess(room, member));
        when(messageRepository.findByTenantIdAndChatRoomIdAndId(tenantId, chatRoomId, lastReadMessageId))
                .thenReturn(Optional.of(lastReadMessage));
        when(clockPort.now()).thenReturn(readAt);

        handler.handle(command);

        verify(chatRoomMemberRepository).update(member);

        ArgumentCaptor<MessageReadEvent> eventCaptor = ArgumentCaptor.forClass(MessageReadEvent.class);
        verify(eventPublisher).publishAfterCommit(eventCaptor.capture());

        assertThat(eventCaptor.getValue()).isEqualTo(
                new MessageReadEvent(tenantId, chatRoomId, userId, lastReadMessageId, readAt)
        );
        assertThat(member.member().getLastReadMessageId()).isEqualTo(lastReadMessageId);
        assertThat(member.member().getLastReadSequence()).isEqualTo(9L);
        assertThat(member.member().getLastReadAt()).isEqualTo(readAt);
    }

    @Test
    void handle_does_not_update_or_publish_for_stale_read_cursor() {
        UUID tenantId = UUID.randomUUID();
        UUID chatRoomId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID currentMessageId = UUID.randomUUID();
        UUID staleMessageId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-04-02T00:00:00Z");
        Instant currentReadAt = createdAt.plusSeconds(30);

        PersistedChatRoom room = persistedRoom(tenantId, chatRoomId, userId, createdAt);
        PersistedChatRoomMember member = persistedMember(
                tenantId,
                chatRoomId,
                userId,
                currentMessageId,
                10L,
                currentReadAt,
                createdAt
        );
        PersistedMessage staleMessage = persistedMessage(tenantId, chatRoomId, userId, staleMessageId, 9L, createdAt);
        MarkReadCommand command = new MarkReadCommand(tenantId, chatRoomId, userId, staleMessageId);

        when(chatRoomAccessSupport.loadReadableActiveMember(tenantId, chatRoomId, userId))
                .thenReturn(new ChatRoomAccess(room, member));
        when(messageRepository.findByTenantIdAndChatRoomIdAndId(tenantId, chatRoomId, staleMessageId))
                .thenReturn(Optional.of(staleMessage));

        handler.handle(command);

        verify(chatRoomMemberRepository, never()).update(any(PersistedChatRoomMember.class));
        verify(eventPublisher, never()).publishAfterCommit(any());

        assertThat(member.member().getLastReadMessageId()).isEqualTo(currentMessageId);
        assertThat(member.member().getLastReadSequence()).isEqualTo(10L);
        assertThat(member.member().getLastReadAt()).isEqualTo(currentReadAt);
    }

    private static PersistedChatRoom persistedRoom(UUID tenantId, UUID chatRoomId, UUID userId, Instant createdAt) {
        ChatRoom chatRoom = ChatRoom.reconstitute(
                chatRoomId,
                RoomType.GROUP,
                new OptionalName("team"),
                userId,
                tenantId,
                null,
                RoomStatus.ACTIVE,
                null,
                null,
                10L,
                null,
                createdAt,
                createdAt
        );
        ChatRoomState state = new ChatRoomState(
                chatRoomId,
                RoomType.GROUP,
                "team",
                userId,
                tenantId,
                null,
                RoomStatus.ACTIVE,
                null,
                null,
                10L,
                null,
                createdAt,
                createdAt,
                0L
        );
        return new PersistedChatRoom(chatRoom, state);
    }

    private static PersistedChatRoomMember persistedMember(
            UUID tenantId,
            UUID chatRoomId,
            UUID userId,
            UUID lastReadMessageId,
            Long lastReadSequence,
            Instant lastReadAt,
            Instant createdAt
    ) {
        UUID memberId = UUID.randomUUID();
        ChatRoomMember member = ChatRoomMember.reconstitute(
                memberId,
                tenantId,
                chatRoomId,
                userId,
                lastReadMessageId,
                lastReadSequence,
                lastReadAt,
                createdAt,
                null,
                createdAt,
                lastReadAt
        );
        ChatRoomMemberState state = new ChatRoomMemberState(
                memberId,
                tenantId,
                chatRoomId,
                userId,
                lastReadMessageId,
                lastReadSequence,
                lastReadAt,
                createdAt,
                null,
                createdAt,
                lastReadAt,
                0L
        );
        return new PersistedChatRoomMember(member, state);
    }

    private static PersistedMessage persistedMessage(
            UUID tenantId,
            UUID chatRoomId,
            UUID senderId,
            UUID messageId,
            long sequence,
            Instant createdAt
    ) {
        MessageState state = new MessageState(
                messageId,
                tenantId,
                chatRoomId,
                senderId,
                UUID.randomUUID(),
                sequence,
                MessageType.USER,
                "hello",
                MessageStatus.ACTIVE,
                null,
                createdAt,
                createdAt,
                null,
                0L
        );
        return new PersistedMessage(Message.reconstitute(state.snapshot()), state);
    }
}
