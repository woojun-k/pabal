package com.polarishb.pabal.messenger.application.port.out.persistence;

import com.polarishb.pabal.messenger.contract.persistence.message.PersistedMessage;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface MessageReadRepository {
    Optional<PersistedMessage> findByTenantIdAndId(UUID tenantId, UUID id);
    Optional<PersistedMessage> findByTenantIdAndChatRoomIdAndId(UUID tenantId, UUID chatRoomId, UUID id);
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
    Map<UUID, Long> countUnreadByRooms(
            UUID tenantId,
            UUID userId,
            Map<UUID, Long> lastReadSequenceByRoomId
    );
    long countUnreadInRoom(
            UUID tenantId,
            UUID chatRoomId,
            UUID userId,
            long lastReadSequence
    );
}
