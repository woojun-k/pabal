package com.polarishb.pabal.messenger.infrastructure.persistence.write;

import com.polarishb.pabal.messenger.contract.persistence.message.MessagePersistenceMapper;
import com.polarishb.pabal.messenger.contract.persistence.message.MessageState;
import com.polarishb.pabal.messenger.contract.persistence.message.PersistedMessage;
import com.polarishb.pabal.messenger.domain.exception.DuplicateMessageException;
import com.polarishb.pabal.messenger.domain.model.entity.Message;
import com.polarishb.pabal.messenger.domain.repository.MessageWriteRepository;
import com.polarishb.pabal.messenger.infrastructure.persistence.jpa.entity.MessageEntity;
import com.polarishb.pabal.messenger.infrastructure.persistence.jpa.write.MessageWriteJpaRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
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
        try {
            MessageEntity saved = jpaRepository.save(MessageEntity.fromNewState(state));
            return MessagePersistenceMapper.toPersisted(saved.toState());
        } catch (DataIntegrityViolationException e) {
            throw translateAppendViolation(state, e);
        }
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

    private RuntimeException translateAppendViolation(MessageState state, DataIntegrityViolationException cause) {
        if (isClientMessageUniqueViolation(cause)) {
            return new DuplicateMessageException(
                    state.chatRoomId(),
                    state.senderId(),
                    state.clientMessageId(),
                    cause
            );
        }
        return cause;
    }

    private boolean isClientMessageUniqueViolation(DataIntegrityViolationException cause) {
        Throwable current = cause;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && message.contains("uq_message_client_id")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
