package com.polarishb.pabal.messenger.infrastructure.persistence.write;

import com.polarishb.pabal.messenger.contract.persistence.message.MessagePersistenceMapper;
import com.polarishb.pabal.messenger.contract.persistence.message.MessageState;
import com.polarishb.pabal.messenger.contract.persistence.message.PersistedMessage;
import com.polarishb.pabal.messenger.domain.model.entity.Message;
import com.polarishb.pabal.messenger.domain.repository.MessageWriteRepository;
import com.polarishb.pabal.messenger.infrastructure.persistence.jpa.entity.MessageEntity;
import com.polarishb.pabal.messenger.infrastructure.persistence.jpa.write.MessageWriteJpaRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Repository
@RequiredArgsConstructor
public class MessageWriteRepositoryImpl implements MessageWriteRepository {

    private final MessageWriteJpaRepository jpaRepository;

    @Override
    @Transactional
    public PersistedMessage append(PersistedMessage persistedMessage) {
        MessageState state = persistedMessage.state();

        MessageEntity saved = jpaRepository.save(MessageEntity.fromNewState(state));
        return MessagePersistenceMapper.toPersisted(saved.toState());
    }

    @Override
    @Transactional
    public PersistedMessage update(PersistedMessage persistedMessage) {
        MessageState currentState = persistedMessage.state();
        Message message = persistedMessage.message();

        MessageEntity entity = jpaRepository.findByTenantIdAndId(
                currentState.tenantId(),
                currentState.id()
        ).orElseThrow(() -> new EntityNotFoundException("Message not found"));

        if (!Objects.equals(entity.getVersion(), currentState.version())) {
            throw new ObjectOptimisticLockingFailureException(
                    MessageEntity.class,
                    currentState.id()
            );
        }

        MessageState nextState = MessagePersistenceMapper.toState(
                message,
                currentState.version()
        );

        entity.apply(nextState);

        return MessagePersistenceMapper.toPersisted(entity.toState());
    }
}