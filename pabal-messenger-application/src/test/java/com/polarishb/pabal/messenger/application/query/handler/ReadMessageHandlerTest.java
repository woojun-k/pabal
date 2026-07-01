package com.polarishb.pabal.messenger.application.query.handler;

import com.polarishb.pabal.messenger.application.query.input.ReadMessageQuery;
import com.polarishb.pabal.messenger.application.query.mapper.MessageQueryMapper;
import com.polarishb.pabal.messenger.application.query.output.MessageDto;
import com.polarishb.pabal.messenger.application.service.ChatRoomReadAccessSupport;
import com.polarishb.pabal.messenger.contract.persistence.chatroom.ChatRoomState;
import com.polarishb.pabal.messenger.contract.persistence.chatroommember.ChatRoomMemberState;
import com.polarishb.pabal.messenger.contract.persistence.message.MessageState;
import com.polarishb.pabal.messenger.domain.model.type.MessageStatus;
import com.polarishb.pabal.messenger.domain.model.type.MessageType;
import com.polarishb.pabal.messenger.domain.model.type.RoomStatus;
import com.polarishb.pabal.messenger.domain.model.type.RoomType;
import com.polarishb.pabal.messenger.application.port.out.persistence.ChatRoomMemberReadRepository;
import com.polarishb.pabal.messenger.application.port.out.persistence.ChatRoomReadRepository;
import com.polarishb.pabal.messenger.application.port.out.persistence.MessageReadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

    private ReadMessageHandler readMessageHandler;

    @BeforeEach
    void setUp() {
        ChatRoomReadAccessSupport chatRoomReadAccessSupport = new ChatRoomReadAccessSupport(
                chatRoomReadRepository,
                chatRoomMemberReadRepository
        );
        readMessageHandler = new ReadMessageHandler(
                messageReadRepository,
                new MessageQueryMapper(),
                chatRoomReadAccessSupport
        );
    }

    @Test
    void handle_returns_message_for_active_member() {
        UUID tenantId = UUID.randomUUID();
        UUID chatRoomId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID clientMessageId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-04-02T12:00:00Z");

        ChatRoomState room = new ChatRoomState(
                chatRoomId,
                RoomType.GROUP,
                "team",
                userId,
                tenantId,
                null,
                RoomStatus.ACTIVE,
                null,
                null,
                0L,
                null,
                createdAt,
                createdAt,
                0L
        );

        ChatRoomMemberState member = new ChatRoomMemberState(
                UUID.randomUUID(),
                tenantId,
                chatRoomId,
                userId,
                null,
                0L,
                null,
                createdAt,
                null,
                createdAt,
                createdAt,
                0L
        );

        MessageState messageState = new MessageState(
                messageId,
                tenantId,
                chatRoomId,
                userId,
                clientMessageId,
                1L,
                MessageType.USER,
                "hello",
                MessageStatus.ACTIVE,
                null,
                createdAt,
                createdAt,
                null,
                0L
        );

        when(chatRoomReadRepository.findByTenantIdAndId(tenantId, chatRoomId))
                .thenReturn(Optional.of(room));
        when(chatRoomMemberReadRepository.findByTenantIdAndChatRoomIdAndUserId(tenantId, chatRoomId, userId))
                .thenReturn(Optional.of(member));
        when(messageReadRepository.findByTenantIdAndChatRoomIdAndId(tenantId, chatRoomId, messageId))
                .thenReturn(Optional.of(messageState));

        MessageDto result = readMessageHandler.handle(new ReadMessageQuery(tenantId, chatRoomId, messageId, userId));

        assertThat(result.messageId()).isEqualTo(messageId);
        assertThat(result.clientMessageId()).isEqualTo(clientMessageId);
        assertThat(result.content()).isEqualTo("hello");
        assertThat(result.status()).isEqualTo("ACTIVE");
    }
}
