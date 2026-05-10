package com.polarishb.pabal.messenger.infrastructure.persistence.write;

import com.polarishb.pabal.messenger.contract.persistence.message.MessagePersistenceMapper;
import com.polarishb.pabal.messenger.contract.persistence.message.MessageState;
import com.polarishb.pabal.messenger.contract.persistence.message.PersistedMessage;
import com.polarishb.pabal.messenger.domain.exception.DuplicateMessageException;
import com.polarishb.pabal.messenger.domain.model.Message;
import com.polarishb.pabal.messenger.application.port.out.persistence.MessageWriteRepository;
import com.polarishb.pabal.messenger.infrastructure.persistence.jpa.entity.MessageEntity;
import com.polarishb.pabal.messenger.infrastructure.persistence.jpa.write.MessageWriteJpaRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Repository
@RequiredArgsConstructor
public class MessageWriteRepositoryImpl implements MessageWriteRepository {

    private static final String CLIENT_MESSAGE_UNIQUE_CONSTRAINT = "uq_message_client_id";

    private final MessageWriteJpaRepository jpaRepository;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public PersistedMessage append(PersistedMessage persistedMessage) {
        MessageState state = persistedMessage.state();
        try {
            MessageEntity saved = jpaRepository.saveAndFlush(MessageEntity.fromNewState(state));
            return MessagePersistenceMapper.toPersisted(saved.toState());
        } catch (DataIntegrityViolationException e) {
            throw translateAppendViolation(state, e);
        }
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
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
            if (current instanceof ConstraintViolationException constraintViolation
                    && CLIENT_MESSAGE_UNIQUE_CONSTRAINT.equals(constraintViolation.getConstraintName())) {
                return true;
            }
            String message = current.getMessage();
            if (message != null && message.contains(CLIENT_MESSAGE_UNIQUE_CONSTRAINT)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
