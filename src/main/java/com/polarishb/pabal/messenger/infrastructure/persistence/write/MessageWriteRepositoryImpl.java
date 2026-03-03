package com.polarishb.pabal.messenger.infrastructure.persistence.write;

import com.polarishb.pabal.messenger.domain.model.entity.Message;
import com.polarishb.pabal.messenger.domain.repository.MessageWriteRepository;
import com.polarishb.pabal.messenger.domain.repository.result.MessageResult;
import com.polarishb.pabal.messenger.infrastructure.persistence.jpa.entity.MessageEntity;
import com.polarishb.pabal.messenger.infrastructure.persistence.jpa.write.MessageWriteJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MessageWriteRepositoryImpl implements MessageWriteRepository {

    private final MessageWriteJpaRepository jpaRepository;

    @Override
    public MessageResult save(Message message) {
        MessageEntity entity = MessageEntity.from(message);
        MessageEntity saved = jpaRepository.save(entity);
        return new MessageResult(
                saved.getId(),
                saved.getTenantId(),
                saved.getChatRoomId(),
                saved.getSenderId(),
                saved.getClientMessageId(),
                saved.getCreatedAt()
        );
    }
}