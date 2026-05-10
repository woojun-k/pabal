package com.polarishb.pabal.messenger.application.port.out.persistence;

import com.polarishb.pabal.messenger.contract.persistence.chatroommember.PersistedChatRoomMember;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatRoomMemberRepository {
    PersistedChatRoomMember append(PersistedChatRoomMember member);
    List<PersistedChatRoomMember> appendAll(List<PersistedChatRoomMember> members);
    PersistedChatRoomMember update(PersistedChatRoomMember member);
    Optional<PersistedChatRoomMember> findByTenantIdAndChatRoomIdAndUserId(UUID tenantId, UUID chatRoomId, UUID userId);
}
