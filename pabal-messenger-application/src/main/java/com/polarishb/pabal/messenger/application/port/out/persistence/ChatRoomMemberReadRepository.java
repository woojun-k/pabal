package com.polarishb.pabal.messenger.application.port.out.persistence;

import com.polarishb.pabal.messenger.contract.persistence.chatroommember.ChatRoomMemberState;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatRoomMemberReadRepository {
    Optional<ChatRoomMemberState> findByTenantIdAndChatRoomIdAndUserId(UUID tenantId, UUID chatRoomId, UUID userId);
    List<ChatRoomMemberState> findAllActiveByTenantIdAndUserId(UUID tenantId, UUID userId);
}
