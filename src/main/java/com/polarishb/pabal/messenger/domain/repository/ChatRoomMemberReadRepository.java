package com.polarishb.pabal.messenger.domain.repository;

import com.polarishb.pabal.messenger.contract.persistence.chatroommember.PersistedChatRoomMember;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatRoomMemberReadRepository {
    Optional<PersistedChatRoomMember> findByChatRoomIdAndUserId(UUID chatRoomId, UUID userId);
    Optional<PersistedChatRoomMember> findByTenantIdAndChatRoomIdAndUserId(UUID tenantId, UUID chatRoomId, UUID userId);
    List<PersistedChatRoomMember> findAllActiveByTenantIdAndUserId(UUID tenantId, UUID userId);
}
