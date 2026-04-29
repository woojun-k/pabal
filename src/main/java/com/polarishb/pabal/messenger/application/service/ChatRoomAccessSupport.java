package com.polarishb.pabal.messenger.application.service;

import com.polarishb.pabal.messenger.application.service.context.ChatRoomAccess;
import com.polarishb.pabal.messenger.contract.persistence.chatroom.PersistedChatRoom;
import com.polarishb.pabal.messenger.contract.persistence.chatroommember.PersistedChatRoomMember;
import com.polarishb.pabal.messenger.domain.exception.ChatRoomNotFoundException;
import com.polarishb.pabal.messenger.domain.exception.MemberNotInRoomException;
import com.polarishb.pabal.messenger.domain.repository.ChatRoomMemberRepository;
import com.polarishb.pabal.messenger.domain.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ChatRoomAccessSupport {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;

    public ChatRoomAccess loadSendableActiveMember(UUID tenantId, UUID chatRoomId, UUID userId) {
        PersistedChatRoom room = loadSendableRoom(tenantId, chatRoomId);
        PersistedChatRoomMember member = loadActiveMember(tenantId, chatRoomId, userId);
        return new ChatRoomAccess(room, member);
    }

    public ChatRoomAccess loadReadableActiveMember(UUID tenantId, UUID chatRoomId, UUID userId) {
        PersistedChatRoom room = loadReadableRoom(tenantId, chatRoomId);
        PersistedChatRoomMember member = loadActiveMember(tenantId, chatRoomId, userId);
        return new ChatRoomAccess(room, member);
    }

    public PersistedChatRoom loadJoinableRoom(UUID tenantId, UUID chatRoomId) {
        PersistedChatRoom room = loadRoom(tenantId, chatRoomId);
        room.chatRoom().validateCanJoin();
        return room;
    }

    public ChatRoomAccess loadLeavableMember(UUID tenantId, UUID chatRoomId, UUID userId) {
        PersistedChatRoom room = loadRoom(tenantId, chatRoomId);
        PersistedChatRoomMember member = loadActiveMember(tenantId, chatRoomId, userId);
        return new ChatRoomAccess(room, member);
    }

    private PersistedChatRoom loadSendableRoom(UUID tenantId, UUID chatRoomId) {
        PersistedChatRoom room = loadRoom(tenantId, chatRoomId);
        room.chatRoom().validateCanSend();
        return room;
    }

    private PersistedChatRoom loadReadableRoom(UUID tenantId, UUID chatRoomId) {
        PersistedChatRoom room = loadRoom(tenantId, chatRoomId);
        room.chatRoom().validateCanRead();
        return room;
    }

    private PersistedChatRoom loadRoom(UUID tenantId, UUID chatRoomId) {
        return chatRoomRepository.findByTenantIdAndId(tenantId, chatRoomId)
                .orElseThrow(() -> new ChatRoomNotFoundException(chatRoomId));
    }

    private PersistedChatRoomMember loadActiveMember(UUID tenantId, UUID chatRoomId, UUID userId) {
        PersistedChatRoomMember member = chatRoomMemberRepository.findByTenantIdAndChatRoomIdAndUserId(
                tenantId,
                chatRoomId,
                userId
        ).orElseThrow(() -> new MemberNotInRoomException(userId));

        member.member().validateActive();
        return member;
    }
}
