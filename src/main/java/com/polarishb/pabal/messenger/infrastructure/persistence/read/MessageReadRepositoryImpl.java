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
    public Optional<Message> findById(UUID id) {
        return jpaRepository.findById(id)
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
}