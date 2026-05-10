package com.polarishb.pabal.messenger.application.port.out.persistence;

import com.polarishb.pabal.messenger.contract.persistence.message.PersistedMessage;

import java.util.Optional;
import java.util.UUID;

public interface MessageRepository {
    PersistedMessage append(PersistedMessage persistedMessage);
    PersistedMessage update(PersistedMessage persistedMessage);
    Optional<PersistedMessage> findByTenantIdAndId(UUID tenantId, UUID id);
    Optional<PersistedMessage> findByTenantIdAndChatRoomIdAndId(UUID tenantId, UUID chatRoomId, UUID id);
    Optional<PersistedMessage> findByTenantIdAndChatRoomIdAndSenderIdAndClientMessageId(UUID tenantId, UUID chatRoomId, UUID senderId, UUID clientMessageId);
}
