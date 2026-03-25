package com.polarishb.pabal.messenger.infrastructure.persistence.jpa.read;

import com.polarishb.pabal.messenger.infrastructure.persistence.jpa.entity.MessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MessageReadJpaRepository extends JpaRepository<MessageEntity, UUID> {
    Optional<MessageEntity> findByTenantIdAndId(UUID tenantId, UUID id);
    Optional<MessageEntity> findByTenantIdAndChatRoomIdAndId(UUID tenantId, UUID chatRoomId, UUID id);
    Optional<MessageEntity> findByChatRoomIdAndSenderIdAndClientMessageId(
            UUID chatRoomId,
            UUID senderId,
            UUID clientMessageId
    );
    Optional<MessageEntity> findByTenantIdAndChatRoomIdAndSenderIdAndClientMessageId(
            UUID tenantId,
            UUID chatRoomId,
            UUID senderId,
            UUID clientMessageId
    );
}