package com.polarishb.pabal.messenger.infrastructure.persistence.jpa.read;

import com.polarishb.pabal.messenger.domain.model.type.MessageStatus;
import com.polarishb.pabal.messenger.infrastructure.persistence.jpa.entity.MessageEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MessageReadJpaRepository extends JpaRepository<MessageEntity, UUID> {
    Optional<MessageEntity> findByTenantIdAndId(UUID tenantId, UUID id);
    Optional<MessageEntity> findByTenantIdAndChatRoomIdAndId(UUID tenantId, UUID chatRoomId, UUID id);
    Optional<MessageEntity> findByTenantIdAndChatRoomIdAndSenderIdAndClientMessageId(
            UUID tenantId,
            UUID chatRoomId,
            UUID senderId,
            UUID clientMessageId
    );

    List<MessageEntity> findByTenantIdAndChatRoomIdOrderBySequenceDesc(UUID tenantId, UUID chatRoomId, Pageable pageable);

    List<MessageEntity> findByTenantIdAndChatRoomIdAndSequenceLessThan(
            UUID tenantId,
            UUID chatRoomId,
            Long sequence,
            Pageable pageable
    );

    @Query("""
            select count(message)
            from MessageEntity message
            where message.tenantId = :tenantId
              and message.chatRoomId = :chatRoomId
              and message.senderId <> :userId
              and message.status <> :deletedStatus
              and message.sequence > :lastReadSequence
            """)
    long countUnreadInRoom(
            @Param("tenantId") UUID tenantId,
            @Param("chatRoomId") UUID chatRoomId,
            @Param("userId") UUID userId,
            @Param("deletedStatus") MessageStatus deletedStatus,
            @Param("lastReadSequence") long lastReadSequence
    );
}
