package com.polarishb.pabal.messenger.application.query.handler;

import com.polarishb.pabal.messenger.application.query.input.ListRoomsQuery;
import com.polarishb.pabal.messenger.application.query.output.RoomDto;
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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListRoomsHandlerTest {

    @Mock
    private ChatRoomMemberReadRepository chatRoomMemberReadRepository;

    @Mock
    private ChatRoomReadRepository chatRoomReadRepository;

    @Mock
    private MessageReadRepository messageReadRepository;

    @InjectMocks
    private ListRoomsHandler listRoomsHandler;

    @Test
    void handle_returns_rooms_sorted_by_last_message_time() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID newerRoomId = UUID.randomUUID();
        UUID olderRoomId = UUID.randomUUID();
        Instant base = Instant.parse("2026-04-02T12:00:00Z");

        PersistedChatRoomMember newerMembership = membership(tenantId, newerRoomId, userId, base);
        PersistedChatRoomMember olderMembership = membership(tenantId, olderRoomId, userId, base.minusSeconds(60));

        PersistedChatRoom newerRoom = room(tenantId, newerRoomId, "newer", 11L, base.plusSeconds(30));
        PersistedChatRoom olderRoom = room(tenantId, olderRoomId, "older", 9L, base.minusSeconds(30));

        when(chatRoomMemberReadRepository.findAllActiveByTenantIdAndUserId(tenantId, userId))
                .thenReturn(List.of(olderMembership, newerMembership));
        when(chatRoomReadRepository.findAllByTenantIdAndIds(tenantId, List.of(olderRoomId, newerRoomId)))
                .thenReturn(List.of(olderRoom, newerRoom));
        when(messageReadRepository.countUnreadInRoom(tenantId, olderRoomId, userId, 0L))
                .thenReturn(1L);
        when(messageReadRepository.countUnreadInRoom(tenantId, newerRoomId, userId, 0L))
                .thenReturn(3L);

        List<RoomDto> result = listRoomsHandler.handle(new ListRoomsQuery(tenantId, userId));

        assertThat(result).hasSize(2);
        assertThat(result.getFirst().roomId()).isEqualTo(newerRoomId);
        assertThat(result.getFirst().unreadCount()).isEqualTo(3L);
        assertThat(result.get(1).roomId()).isEqualTo(olderRoomId);
    }

    private static PersistedChatRoomMember membership(UUID tenantId, UUID roomId, UUID userId, Instant joinedAt) {
        ChatRoomMember member = ChatRoomMember.reconstitute(
                UUID.randomUUID(),
                tenantId,
                roomId,
                userId,
                null,
                0L,
                null,
                joinedAt,
                null,
                joinedAt,
                joinedAt
        );

        return new PersistedChatRoomMember(
                member,
                new ChatRoomMemberState(
                        member.getId(),
                        tenantId,
                        roomId,
                        userId,
                        null,
                        0L,
                        null,
                        joinedAt,
                        null,
                        joinedAt,
                        joinedAt,
                        0L
                )
        );
    }

    private static PersistedChatRoom room(UUID tenantId, UUID roomId, String name, long lastMessageSequence, Instant lastMessageAt) {
        ChatRoom chatRoom = ChatRoom.reconstitute(
                roomId,
                RoomType.GROUP,
                new OptionalName(name),
                UUID.randomUUID(),
                tenantId,
                null,
                RoomStatus.ACTIVE,
                null,
                UUID.randomUUID(),
                lastMessageSequence,
                lastMessageAt,
                lastMessageAt.minusSeconds(60),
                lastMessageAt
        );

        return new PersistedChatRoom(
                chatRoom,
                new ChatRoomState(
                        roomId,
                        RoomType.GROUP,
                        name,
                        chatRoom.getCreatedBy(),
                        tenantId,
                        null,
                        RoomStatus.ACTIVE,
                        null,
                        chatRoom.getLastMessageId(),
                        lastMessageSequence,
                        lastMessageAt,
                        lastMessageAt.minusSeconds(60),
                        lastMessageAt,
                        0L
                )
        );
    }
}
