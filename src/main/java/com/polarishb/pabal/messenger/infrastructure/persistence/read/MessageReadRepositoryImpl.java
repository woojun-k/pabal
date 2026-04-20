package com.polarishb.pabal.messenger.infrastructure.persistence.read;

import com.polarishb.pabal.messenger.contract.persistence.message.MessagePersistenceMapper;
import com.polarishb.pabal.messenger.contract.persistence.message.PersistedMessage;
import com.polarishb.pabal.messenger.domain.model.type.MessageStatus;
import com.polarishb.pabal.messenger.domain.repository.MessageReadRepository;
import com.polarishb.pabal.messenger.infrastructure.persistence.jpa.entity.MessageEntity;
import com.polarishb.pabal.messenger.infrastructure.persistence.jpa.read.MessageReadJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class MessageReadRepositoryImpl implements MessageReadRepository {

    private final MessageReadJpaRepository jpaRepository;

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
    public Optional<PersistedMessage> findByChatRoomIdAndSenderIdAndClientMessageId(
            UUID chatRoomId,
            UUID senderId,
            UUID clientMessageId
    ) {
        return jpaRepository
                .findByChatRoomIdAndSenderIdAndClientMessageId(
                        chatRoomId,
                        senderId,
                        clientMessageId
                )
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
