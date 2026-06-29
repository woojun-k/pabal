package com.polarishb.pabal.messenger.application.service;

import com.polarishb.pabal.messenger.application.port.out.identity.RoomParticipantDirectoryPort;
import com.polarishb.pabal.messenger.application.port.out.persistence.ChatRoomMemberRepository;
import com.polarishb.pabal.messenger.application.port.out.persistence.ChatRoomRepository;
import com.polarishb.pabal.messenger.application.service.context.ChatRoomAccess;
import com.polarishb.pabal.messenger.contract.persistence.chatroom.ChatRoomPersistenceMapper;
import com.polarishb.pabal.messenger.contract.persistence.chatroom.PersistedChatRoom;
import com.polarishb.pabal.messenger.contract.persistence.chatroommember.ChatRoomMemberPersistenceMapper;
import com.polarishb.pabal.messenger.contract.persistence.chatroommember.PersistedChatRoomMember;
import com.polarishb.pabal.messenger.domain.exception.MemberNotActiveException;
import com.polarishb.pabal.messenger.domain.model.ChatRoom;
import com.polarishb.pabal.messenger.domain.model.ChatRoomMember;
import com.polarishb.pabal.messenger.domain.model.type.RoomStatus;
import com.polarishb.pabal.messenger.domain.model.type.RoomType;
import com.polarishb.pabal.messenger.domain.model.vo.OptionalName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatRoomAccessSupportTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatRoomMemberRepository chatRoomMemberRepository;

    @Mock
    private RoomParticipantDirectoryPort participantDirectoryPort;

    @InjectMocks
    private ChatRoomAccessSupport chatRoomAccessSupport;

    @Test
    void loadSendableActiveMember_returns_access_when_room_member_and_user_are_active() {
        UUID tenantId = UUID.randomUUID();
        UUID chatRoomId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant now = Instant.parse("2026-04-02T12:00:00Z");
        PersistedChatRoom room = activeRoom(tenantId, chatRoomId, userId, now);
        PersistedChatRoomMember member = activeMember(tenantId, chatRoomId, userId, now);

        when(chatRoomRepository.findByTenantIdAndId(tenantId, chatRoomId))
                .thenReturn(Optional.of(room));
        when(chatRoomMemberRepository.findByTenantIdAndChatRoomIdAndUserId(tenantId, chatRoomId, userId))
                .thenReturn(Optional.of(member));
        when(participantDirectoryPort.existsActiveTenantMember(tenantId, userId)).thenReturn(true);

        ChatRoomAccess access = chatRoomAccessSupport.loadSendableActiveMember(tenantId, chatRoomId, userId);

        assertThat(access.room()).isEqualTo(room);
        assertThat(access.member()).isEqualTo(member);
        verify(participantDirectoryPort).existsActiveTenantMember(tenantId, userId);
    }

    @Test
    void loadSendableActiveMember_rejects_inactive_tenant_user_even_when_room_member_is_active() {
        UUID tenantId = UUID.randomUUID();
        UUID chatRoomId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant now = Instant.parse("2026-04-02T12:00:00Z");

        when(chatRoomRepository.findByTenantIdAndId(tenantId, chatRoomId))
                .thenReturn(Optional.of(activeRoom(tenantId, chatRoomId, userId, now)));
        when(chatRoomMemberRepository.findByTenantIdAndChatRoomIdAndUserId(tenantId, chatRoomId, userId))
                .thenReturn(Optional.of(activeMember(tenantId, chatRoomId, userId, now)));
        when(participantDirectoryPort.existsActiveTenantMember(tenantId, userId)).thenReturn(false);

        assertThatThrownBy(() -> chatRoomAccessSupport.loadSendableActiveMember(tenantId, chatRoomId, userId))
                .isInstanceOf(MemberNotActiveException.class);
    }

    private static PersistedChatRoom activeRoom(UUID tenantId, UUID chatRoomId, UUID createdBy, Instant now) {
        ChatRoom room = ChatRoom.reconstitute(
                chatRoomId,
                RoomType.GROUP,
                new OptionalName("team"),
                createdBy,
                tenantId,
                null,
                RoomStatus.ACTIVE,
                null,
                null,
                0L,
                null,
                now,
                now
        );
        return new PersistedChatRoom(room, ChatRoomPersistenceMapper.toState(room, 0L));
    }

    private static PersistedChatRoomMember activeMember(UUID tenantId, UUID chatRoomId, UUID userId, Instant now) {
        ChatRoomMember member = ChatRoomMember.reconstitute(
                UUID.randomUUID(),
                tenantId,
                chatRoomId,
                userId,
                null,
                0L,
                null,
                now,
                null,
                now,
                now
        );
        return new PersistedChatRoomMember(member, ChatRoomMemberPersistenceMapper.toState(member, 0L));
    }
}
