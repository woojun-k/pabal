package com.polarishb.pabal.messenger.domain.repository;

import com.polarishb.pabal.messenger.contract.persistence.message.PersistedMessage;

import java.util.List;
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
    List<PersistedMessage> findByTenantIdAndChatRoomIdBeforeSequence(
            UUID tenantId,
            UUID chatRoomId,
            Long cursor,
            int limit
    );
    long countUnreadInRoom(
            UUID tenantId,
            UUID chatRoomId,
            UUID userId,
            long lastReadSequence
    );
}
