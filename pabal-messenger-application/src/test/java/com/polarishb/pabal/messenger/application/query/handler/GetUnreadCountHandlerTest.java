package com.polarishb.pabal.messenger.application.query.handler;

import com.polarishb.pabal.messenger.application.query.input.GetUnreadCountQuery;
import com.polarishb.pabal.messenger.application.query.output.UnreadCountResult;
import com.polarishb.pabal.messenger.application.service.ChatRoomReadAccessSupport;
import com.polarishb.pabal.messenger.contract.persistence.chatroom.ChatRoomState;
import com.polarishb.pabal.messenger.contract.persistence.chatroommember.ChatRoomMemberState;
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
class GetUnreadCountHandlerTest {

    @Mock
    private ChatRoomReadRepository chatRoomReadRepository;

    @Mock
    private ChatRoomMemberReadRepository chatRoomMemberReadRepository;

    @Mock
    private MessageReadRepository messageReadRepository;

    private GetUnreadCountHandler getUnreadCountHandler;

    @BeforeEach
    void setUp() {
        ChatRoomReadAccessSupport chatRoomReadAccessSupport = new ChatRoomReadAccessSupport(
                chatRoomReadRepository,
                chatRoomMemberReadRepository
        );
        getUnreadCountHandler = new GetUnreadCountHandler(messageReadRepository, chatRoomReadAccessSupport);
    }

    @Test
    void handle_uses_last_read_sequence_as_cursor() {
        UUID tenantId = UUID.randomUUID();
        UUID chatRoomId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-04-02T12:00:00Z");
        long lastReadSequence = 7L;

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
                12L,
                null,
                createdAt,
                createdAt,
                0L
        );

        UUID lastReadMessageId = UUID.randomUUID();
        Instant lastReadAt = createdAt.plusSeconds(30);
        ChatRoomMemberState member = new ChatRoomMemberState(
                UUID.randomUUID(),
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

        when(chatRoomReadRepository.findByTenantIdAndId(tenantId, chatRoomId)).thenReturn(Optional.of(room));
        when(chatRoomMemberReadRepository.findByTenantIdAndChatRoomIdAndUserId(tenantId, chatRoomId, userId))
                .thenReturn(Optional.of(member));
        when(messageReadRepository.countUnreadInRoom(tenantId, chatRoomId, userId, lastReadSequence))
                .thenReturn(4L);

        UnreadCountResult result = getUnreadCountHandler.handle(
                new GetUnreadCountQuery(tenantId, chatRoomId, userId)
        );

        assertThat(result.unreadCount()).isEqualTo(4L);
    }
}
