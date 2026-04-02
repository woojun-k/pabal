package com.polarishb.pabal.messenger.domain.repository;

import com.polarishb.pabal.messenger.contract.persistence.message.PersistedMessage;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface MessageReadRepository {
    Optional<PersistedMessage> findByTenantIdAndId(UUID tenantId, UUID id);
    Optional<PersistedMessage> findByTenantIdAndChatRoomIdAndId(UUID tenantId, UUID chatRoomId, UUID id);
    Optional<PersistedMessage> findByChatRoomIdAndSenderIdAndClientMessageId(
            UUID chatRoomId,
            UUID senderId,
            UUID clientMessageId
    );
    Optional<PersistedMessage> findByTenantIdAndChatRoomIdAndSenderIdAndClientMessageId(
            UUID tenantId,
            UUID chatRoomId,
            UUID senderId,
            UUID clientMessageId
    );
    long countUnreadInRoom(
            UUID tenantId,
            UUID chatRoomId,
            UUID userId,
            Instant readThreshold
    );
}
