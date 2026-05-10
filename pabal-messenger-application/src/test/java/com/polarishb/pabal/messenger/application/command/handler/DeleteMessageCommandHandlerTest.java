package com.polarishb.pabal.messenger.application.command.handler;

import com.polarishb.pabal.common.event.DomainEventPublisher;
import com.polarishb.pabal.messenger.application.command.input.DeleteMessageCommand;
import com.polarishb.pabal.messenger.application.port.out.persistence.MessageRepository;
import com.polarishb.pabal.messenger.application.port.out.time.ClockPort;
import com.polarishb.pabal.messenger.application.service.ChatRoomAccessSupport;
import com.polarishb.pabal.messenger.contract.persistence.message.MessagePersistenceMapper;
import com.polarishb.pabal.messenger.contract.persistence.message.MessageState;
import com.polarishb.pabal.messenger.contract.persistence.message.PersistedMessage;
import com.polarishb.pabal.messenger.domain.exception.MemberNotInRoomException;
import com.polarishb.pabal.messenger.domain.exception.RoomOperationNotAllowedException;
import com.polarishb.pabal.messenger.domain.model.type.MessageStatus;
import com.polarishb.pabal.messenger.domain.model.type.MessageType;
import com.polarishb.pabal.messenger.domain.model.type.RoomAccessOperation;
import com.polarishb.pabal.messenger.domain.model.type.RoomStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeleteMessageCommandHandlerTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private DomainEventPublisher eventPublisher;

    @Mock
    private ClockPort clockPort;

    @Mock
    private ChatRoomAccessSupport chatRoomAccessSupport;

    @InjectMocks
    private DeleteMessageCommandHandler handler;

    @Test
    void handle_rechecks_sendable_active_membership_before_deleting_message() {
        UUID tenantId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        PersistedMessage message = persistedMessage(tenantId, roomId, messageId, requesterId);
        DeleteMessageCommand command = new DeleteMessageCommand(tenantId, roomId, messageId, requesterId);

        when(messageRepository.findByTenantIdAndChatRoomIdAndId(tenantId, roomId, messageId))
                .thenReturn(Optional.of(message));
        when(clockPort.now()).thenReturn(Instant.parse("2026-04-02T12:05:00Z"));
        when(messageRepository.update(any(PersistedMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        handler.handle(command);

        InOrder inOrder = inOrder(messageRepository, chatRoomAccessSupport);
        inOrder.verify(messageRepository).findByTenantIdAndChatRoomIdAndId(tenantId, roomId, messageId);
        inOrder.verify(chatRoomAccessSupport).loadSendableActiveMember(tenantId, roomId, requesterId);
        inOrder.verify(messageRepository).update(message);
    }

    @Test
    void handle_rejects_delete_when_sender_is_no_longer_active_member() {
        UUID tenantId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        PersistedMessage message = persistedMessage(tenantId, roomId, messageId, requesterId);

        when(messageRepository.findByTenantIdAndChatRoomIdAndId(tenantId, roomId, messageId))
                .thenReturn(Optional.of(message));
        when(chatRoomAccessSupport.loadSendableActiveMember(tenantId, roomId, requesterId))
                .thenThrow(new MemberNotInRoomException(requesterId));

        assertThatThrownBy(() -> handler.handle(new DeleteMessageCommand(tenantId, roomId, messageId, requesterId)))
                .isInstanceOf(MemberNotInRoomException.class);

        verify(clockPort, never()).now();
        verify(messageRepository, never()).update(any());
        verify(eventPublisher, never()).publishAfterCommit(any());
    }

    @Test
    void handle_rejects_delete_when_room_is_not_sendable() {
        UUID tenantId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        PersistedMessage message = persistedMessage(tenantId, roomId, messageId, requesterId);

        when(messageRepository.findByTenantIdAndChatRoomIdAndId(tenantId, roomId, messageId))
                .thenReturn(Optional.of(message));
        when(chatRoomAccessSupport.loadSendableActiveMember(tenantId, roomId, requesterId))
                .thenThrow(new RoomOperationNotAllowedException(
                        roomId,
                        RoomStatus.PENDING_DELETION,
                        RoomAccessOperation.SEND
                ));

        assertThatThrownBy(() -> handler.handle(new DeleteMessageCommand(tenantId, roomId, messageId, requesterId)))
                .isInstanceOf(RoomOperationNotAllowedException.class);

        verify(clockPort, never()).now();
        verify(messageRepository, never()).update(any());
        verify(eventPublisher, never()).publishAfterCommit(any());
    }

    private PersistedMessage persistedMessage(UUID tenantId, UUID roomId, UUID messageId, UUID senderId) {
        Instant createdAt = Instant.parse("2026-04-02T12:00:00Z");
        MessageState state = new MessageState(
                messageId,
                tenantId,
                roomId,
                senderId,
                UUID.randomUUID(),
                10L,
                MessageType.USER,
                "original",
                MessageStatus.ACTIVE,
                null,
                createdAt,
                createdAt,
                null,
                0L
        );
        return MessagePersistenceMapper.toPersisted(state);
    }
}
