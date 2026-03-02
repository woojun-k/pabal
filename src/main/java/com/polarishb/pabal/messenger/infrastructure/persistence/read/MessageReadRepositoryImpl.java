package com.polarishb.pabal.messenger.infrastructure.persistence.read;

import com.polarishb.pabal.messenger.domain.model.entity.Message;
import com.polarishb.pabal.messenger.domain.repository.MessageReadRepository;
import com.polarishb.pabal.messenger.infrastructure.persistence.jpa.entity.MessageEntity;
import com.polarishb.pabal.messenger.infrastructure.persistence.jpa.read.MessageReadJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class MessageReadRepositoryImpl implements MessageReadRepository {

    private final MessageReadJpaRepository jpaRepository;

    @Override
    public Optional<Message> findByTenantIdAndId(UUID tenantId, UUID id) {
        return jpaRepository.findByTenantIdAndId(tenantId, id)
                .map(MessageEntity::toDomain);
    }

    @Override
    public Optional<Message> findByChatRoomIdAndId(UUID chatRoomId, UUID id) {
        return jpaRepository.findByChatRoomIdAndId(chatRoomId, id)
                .map(MessageEntity::toDomain);
    }

    @Override
    public Optional<Message> findByChatRoomIdAndSenderIdAndClientMessageId(
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
                .map(MessageEntity::toDomain);
    }

    @Override
    public Optional<Message> findByTenantIdAndChatRoomIdAndSenderIdAndClientMessageId(
            UUID tenantId,
            UUID chatRoomId,
            UUID senderId,
            UUID clientMessageId
    ) {
        return jpaRepository.findByTenantIdAndChatRoomIdAndSenderIdAndClientMessageId(
            tenantId,
            chatRoomId,
            senderId,
            clientMessageId
        )
        .map(MessageEntity::toDomain);
    }
}