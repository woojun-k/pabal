package com.polarishb.pabal.messenger.application.query.handler;

import com.polarishb.pabal.messenger.application.query.input.GetUnreadCountQuery;
import com.polarishb.pabal.messenger.application.query.output.UnreadCountResult;
import com.polarishb.pabal.messenger.contract.persistence.chatroom.ChatRoomState;
import com.polarishb.pabal.messenger.contract.persistence.chatroom.PersistedChatRoom;
import com.polarishb.pabal.messenger.contract.persistence.chatroommember.ChatRoomMemberState;
import com.polarishb.pabal.messenger.contract.persistence.chatroommember.PersistedChatRoomMember;
import com.polarishb.pabal.messenger.domain.model.entity.ChatRoom;
import com.polarishb.pabal.messenger.domain.model.entity.ChatRoomMember;
import com.polarishb.pabal.messenger.domain.model.type.RoomStatus;
import com.polarishb.pabal.messenger.domain.model.type.RoomType;
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
class GetUnreadCountHandlerTest {

    @Mock
    private ChatRoomReadRepository chatRoomReadRepository;

    @Mock
    private ChatRoomMemberReadRepository chatRoomMemberReadRepository;

    @Mock
    private MessageReadRepository messageReadRepository;

    @InjectMocks
    private GetUnreadCountHandler getUnreadCountHandler;

    @Test
    void handle_uses_last_read_sequence_as_cursor() {
        UUID tenantId = UUID.randomUUID();
        UUID chatRoomId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-04-02T12:00:00Z");
        long lastReadSequence = 7L;

        PersistedChatRoom room = new PersistedChatRoom(
                ChatRoom.reconstitute(
                        chatRoomId,
                        RoomType.GROUP,
                        new OptionalName("team"),
                        userId,
                        tenantId,
                        null,
                        RoomStatus.ACTIVE,
                        null,
                        null,
                        12L,
                        null,
                        createdAt,
                        createdAt
                ),
                new ChatRoomState(
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
                )
        );

        ChatRoomMember member = ChatRoomMember.reconstitute(
                UUID.randomUUID(),
                tenantId,
                chatRoomId,
                userId,
                UUID.randomUUID(),
                lastReadSequence,
                createdAt.plusSeconds(30),
                createdAt,
                null,
                createdAt,
                createdAt.plusSeconds(30)
        );
        PersistedChatRoomMember persistedMember = new PersistedChatRoomMember(
                member,
                new ChatRoomMemberState(
                        member.getId(),
                        tenantId,
                        chatRoomId,
                        userId,
                        member.getLastReadMessageId(),
                        lastReadSequence,
                        member.getLastReadAt(),
                        createdAt,
                        null,
                        createdAt,
                        member.getLastReadAt(),
                        0L
                )
        );

        when(chatRoomReadRepository.findByTenantIdAndId(tenantId, chatRoomId)).thenReturn(Optional.of(room));
        when(chatRoomMemberReadRepository.findByTenantIdAndChatRoomIdAndUserId(tenantId, chatRoomId, userId))
                .thenReturn(Optional.of(persistedMember));
        when(messageReadRepository.countUnreadInRoom(tenantId, chatRoomId, userId, lastReadSequence))
                .thenReturn(4L);

        UnreadCountResult result = getUnreadCountHandler.handle(
                new GetUnreadCountQuery(tenantId, chatRoomId, userId)
        );

        assertThat(result.unreadCount()).isEqualTo(4L);
    }
}
