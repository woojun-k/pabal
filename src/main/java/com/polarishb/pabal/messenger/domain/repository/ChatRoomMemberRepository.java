package com.polarishb.pabal.messenger.domain.repository;

import com.polarishb.pabal.messenger.domain.model.entity.ChatRoomMember;

import java.util.Optional;
import java.util.UUID;

public interface ChatRoomMemberRepository {
    void save(ChatRoomMember member);
    Optional<ChatRoomMember> findByChatRoomIdAndUserId(UUID chatRoomId, UUID userId);
    Optional<ChatRoomMember> findByTenantIdAndChatRoomIdAndUserId(UUID tenantId, UUID chatRoomId, UUID userId);
}