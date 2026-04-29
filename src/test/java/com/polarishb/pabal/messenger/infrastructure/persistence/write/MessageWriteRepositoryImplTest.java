package com.polarishb.pabal.messenger.infrastructure.persistence.write;

import com.polarishb.pabal.messenger.contract.persistence.message.MessageState;
import com.polarishb.pabal.messenger.contract.persistence.message.PersistedMessage;
import com.polarishb.pabal.messenger.domain.exception.DuplicateMessageException;
import com.polarishb.pabal.messenger.domain.model.entity.Message;
import com.polarishb.pabal.messenger.domain.model.type.MessageStatus;
import com.polarishb.pabal.messenger.domain.model.type.MessageType;
import com.polarishb.pabal.messenger.infrastructure.persistence.jpa.write.MessageWriteJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageWriteRepositoryImplTest {

    @Mock
    private MessageWriteJpaRepository jpaRepository;

    @InjectMocks
    private MessageWriteRepositoryImpl repository;

    @Test
    void append_translates_client_message_unique_violation_to_duplicate_message() {
        DataIntegrityViolationException cause = new DataIntegrityViolationException(
                "could not execute statement",
                new RuntimeException("duplicate key violates constraint uq_message_client_id")
        );
        when(jpaRepository.saveAndFlush(any())).thenThrow(cause);

        assertThatThrownBy(() -> repository.append(draftMessage()))
                .isInstanceOf(DuplicateMessageException.class)
                .hasCause(cause);
    }

    private PersistedMessage draftMessage() {
        Instant now = Instant.parse("2026-04-02T12:00:00Z");
        MessageState state = new MessageState(
                null,
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                1L,
                MessageType.USER,
                "hello",
                MessageStatus.ACTIVE,
                null,
                now,
                now,
                null,
                null
        );
        return new PersistedMessage(Message.reconstitute(state.snapshot()), state);
    }
}
