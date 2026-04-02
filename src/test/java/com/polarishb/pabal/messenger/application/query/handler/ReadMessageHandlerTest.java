package com.polarishb.pabal.messenger.application.query.handler;

import com.polarishb.pabal.messenger.application.query.input.ReadMessageQuery;
import com.polarishb.pabal.messenger.application.query.output.MessageDto;
import com.polarishb.pabal.messenger.contract.persistence.chatroom.PersistedChatRoom;
import com.polarishb.pabal.messenger.contract.persistence.chatroommember.ChatRoomMemberState;
import com.polarishb.pabal.messenger.contract.persistence.chatroommember.PersistedChatRoomMember;
import com.polarishb.pabal.messenger.contract.persistence.message.MessageState;
import com.polarishb.pabal.messenger.contract.persistence.message.PersistedMessage;
import com.polarishb.pabal.messenger.domain.model.entity.ChatRoom;
import com.polarishb.pabal.messenger.domain.model.entity.ChatRoomMember;
import com.polarishb.pabal.messenger.domain.model.entity.Message;
import com.polarishb.pabal.messenger.domain.model.type.MessageStatus;
import com.polarishb.pabal.messenger.domain.model.type.MessageType;
import com.polarishb.pabal.messenger.domain.model.type.RoomStatus;
import com.polarishb.pabal.messenger.domain.model.type.RoomType;
import com.polarishb.pabal.messenger.domain.model.vo.MessageContent;
import com.polarishb.pabal.messenger.domain.model.vo.OptionalName;
import com.polarishb.pabal.messenger.domain.repository.ChatRoomMemberReadRepository;
import com.polarishb.pabal.messenger.domain.repository.ChatRoomReadRepository;
import com.polarishb.pabal.messenger.domain.repository.MessageReadRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReadMessageHandlerTest {

    @Mock
    private ChatRoomReadRepository chatRoomReadRepository;

    @Mock
    private ChatRoomMemberReadRepository chatRoomMemberReadRepository;

    @Mock
    private MessageReadRepository messageReadRepository;

    @InjectMocks
    private ReadMessageHandler readMessageHandler;

    @Test
    void handle_returns_message_for_active_member() {
        UUID tenantId = UUID.randomUUID();
        UUID chatRoomId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID clientMessageId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-04-02T12:00:00Z");

        ChatRoom room = ChatRoom.reconstitute(
                chatRoomId,
                RoomType.GROUP,
                new OptionalName("team"),
                userId,
                tenantId,
                null,
                RoomStatus.ACTIVE,
                null,
                null,
                null,
                createdAt,
                createdAt
        );
        PersistedChatRoom persistedRoom = new PersistedChatRoom(room, nullSafeRoomState(room, createdAt));

        ChatRoomMember member = ChatRoomMember.reconstitute(
                UUID.randomUUID(),
                tenantId,
                chatRoomId,
                userId,
                null,
                null,
                createdAt,
                null,
                createdAt,
                createdAt
        );
        PersistedChatRoomMember persistedMember = new PersistedChatRoomMember(
                member,
                new ChatRoomMemberState(
                        member.getId(),
                        tenantId,
                        chatRoomId,
                        userId,
                        null,
                        null,
                        createdAt,
                        null,
                        createdAt,
                        createdAt,
                        0L
                )
        );

        Message message = Message.reconstitute(
                messageId,
                tenantId,
                chatRoomId,
                userId,
                clientMessageId,
                MessageType.USER,
                new MessageContent("hello"),
                MessageStatus.ACTIVE,
                null,
                createdAt,
                createdAt,
                null
        );
        PersistedMessage persistedMessage = new PersistedMessage(
                message,
                new MessageState(
                        messageId,
                        tenantId,
                        chatRoomId,
                        userId,
                        clientMessageId,
                        MessageType.USER,
                        "hello",
                        MessageStatus.ACTIVE,
                        null,
                        createdAt,
                        createdAt,
                        null,
                        0L
                )
        );

        when(chatRoomReadRepository.findByTenantIdAndId(tenantId, chatRoomId))
                .thenReturn(Optional.of(persistedRoom));
        when(chatRoomMemberReadRepository.findByTenantIdAndChatRoomIdAndUserId(tenantId, chatRoomId, userId))
                .thenReturn(Optional.of(persistedMember));
        when(messageReadRepository.findByTenantIdAndChatRoomIdAndId(tenantId, chatRoomId, messageId))
                .thenReturn(Optional.of(persistedMessage));

        MessageDto result = readMessageHandler.handle(new ReadMessageQuery(tenantId, chatRoomId, messageId, userId));

        assertThat(result.messageId()).isEqualTo(messageId);
        assertThat(result.clientMessageId()).isEqualTo(clientMessageId);
        assertThat(result.content()).isEqualTo("hello");
        assertThat(result.status()).isEqualTo("ACTIVE");
    }

    private static com.polarishb.pabal.messenger.contract.persistence.chatroom.ChatRoomState nullSafeRoomState(
            ChatRoom room,
            Instant createdAt
    ) {
        return new com.polarishb.pabal.messenger.contract.persistence.chatroom.ChatRoomState(
                room.getId(),
                room.getType(),
                room.getName().valueOrNull(),
                room.getCreatedBy(),
                room.getTenantId(),
                room.getChannelSettings(),
                room.getStatus(),
                room.getScheduledDeletionAt(),
                room.getLastMessageId(),
                room.getLastMessageAt(),
                createdAt,
                createdAt,
                0L
        );
    }
}
