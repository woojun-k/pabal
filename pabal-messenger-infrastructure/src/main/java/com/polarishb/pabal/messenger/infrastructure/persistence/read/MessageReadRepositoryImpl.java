package com.polarishb.pabal.messenger.infrastructure.persistence.read;

import com.polarishb.pabal.messenger.contract.persistence.message.MessagePersistenceMapper;
import com.polarishb.pabal.messenger.contract.persistence.message.PersistedMessage;
import com.polarishb.pabal.messenger.domain.model.type.MessageStatus;
import com.polarishb.pabal.messenger.application.port.out.persistence.MessageReadRepository;
import com.polarishb.pabal.messenger.infrastructure.persistence.jpa.entity.MessageEntity;
import com.polarishb.pabal.messenger.infrastructure.persistence.jpa.read.MessageReadJpaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class MessageReadRepositoryImpl implements MessageReadRepository {

    private final MessageReadJpaRepository jpaRepository;
    private final EntityManager entityManager;

    @Override
    public Optional<PersistedMessage> findByTenantIdAndId(UUID tenantId, UUID id) {
        return jpaRepository.findByTenantIdAndId(tenantId, id)
                .map(MessageEntity::toState)
                .map(MessagePersistenceMapper::toPersisted);
    }

    @Override
    public Optional<PersistedMessage> findByTenantIdAndChatRoomIdAndId(UUID tenantId, UUID chatRoomId, UUID id) {
        return jpaRepository.findByTenantIdAndChatRoomIdAndId(tenantId, chatRoomId, id)
                .map(MessageEntity::toState)
                .map(MessagePersistenceMapper::toPersisted);
    }

    @Override
    public Optional<PersistedMessage> findByTenantIdAndChatRoomIdAndSenderIdAndClientMessageId(
            UUID tenantId,
            UUID chatRoomId,
            UUID senderId,
            UUID clientMessageId
    ) {
        return jpaRepository
                .findByTenantIdAndChatRoomIdAndSenderIdAndClientMessageId(
                    tenantId,
                    chatRoomId,
                    senderId,
                    clientMessageId
                )
                .map(MessageEntity::toState)
                .map(MessagePersistenceMapper::toPersisted);
    }

    @Override
    public List<PersistedMessage> findByTenantIdAndChatRoomIdBeforeSequence(
            UUID tenantId,
            UUID chatRoomId,
            Long cursor,
            int limit
    ) {
        PageRequest pageable = PageRequest.of(
                0,
                limit,
                Sort.by(Sort.Direction.DESC, "sequence")
        );

        List<MessageEntity> messages = cursor == null
                ? jpaRepository.findByTenantIdAndChatRoomIdOrderBySequenceDesc(tenantId, chatRoomId, pageable)
                : jpaRepository.findByTenantIdAndChatRoomIdAndSequenceLessThan(
                tenantId,
                chatRoomId,
                cursor,
                pageable
        );

        return messages.stream()
                .map(MessageEntity::toState)
                .map(MessagePersistenceMapper::toPersisted)
                .toList();
    }

    @Override
    public Map<UUID, Long> countUnreadByRooms(
            UUID tenantId,
            UUID userId,
            Map<UUID, Long> lastReadSequenceByRoomId
    ) {
        if (lastReadSequenceByRoomId == null || lastReadSequenceByRoomId.isEmpty()) {
            return Map.of();
        }

        String thresholdTable = buildThresholdTable(lastReadSequenceByRoomId);

        String sql = """
                SELECT
                    m.chat_room_id AS chat_room_id,
                    COUNT(*) AS unread_count
                FROM message m
                JOIN (
                    %s
                ) threshold_table
                    ON threshold_table.chat_room_id = m.chat_room_id
                WHERE m.tenant_id = :tenantId
                  AND m.sequence > threshold_table.last_read_sequence
                  AND m.sender_id <> :userId
                  AND m.status <> :deletedStatus
                GROUP BY m.chat_room_id
                """.formatted(thresholdTable);

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("tenantId", tenantId);
        query.setParameter("userId", userId);
        query.setParameter("deletedStatus", MessageStatus.DELETED.name());

        int index = 0;
        for (Map.Entry<UUID, Long> entry : lastReadSequenceByRoomId.entrySet()) {
            query.setParameter("roomId" + index, entry.getKey());
            query.setParameter("lastReadSequence" + index, entry.getValue());
            index++;
        }

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();

        return rows.stream()
                .collect(Collectors.toMap(
                        row -> (UUID) row[0],
                        row -> ((Number) row[1]).longValue()
                ));
    }

    private String buildThresholdTable(Map<UUID, Long> lastReadSequenceByRoomId) {
        StringBuilder builder = new StringBuilder();

        int index = 0;
        for (UUID ignored : lastReadSequenceByRoomId.keySet()) {
            if (index > 0) {
                builder.append("\nUNION ALL\n");
            }

            builder.append("""
                    SELECT
                        CAST(:roomId%d AS uuid) AS chat_room_id,
                        CAST(:lastReadSequence%d AS bigint) AS last_read_sequence
                    """.formatted(index, index));

            index++;
        }

        return builder.toString();
    }

    @Override
    public long countUnreadInRoom(UUID tenantId, UUID chatRoomId, UUID userId, long lastReadSequence) {
        return jpaRepository.countUnreadInRoom(
                tenantId,
                chatRoomId,
                userId,
                MessageStatus.DELETED,
                lastReadSequence
        );
    }
}
