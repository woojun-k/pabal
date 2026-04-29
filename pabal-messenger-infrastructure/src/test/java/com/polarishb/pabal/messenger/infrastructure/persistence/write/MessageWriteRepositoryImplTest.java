package com.polarishb.pabal.messenger.infrastructure.persistence.write;

import com.polarishb.pabal.messenger.application.port.out.persistence.MessageWriteRepository;
import com.polarishb.pabal.messenger.contract.persistence.message.MessageState;
import com.polarishb.pabal.messenger.contract.persistence.message.PersistedMessage;
import com.polarishb.pabal.messenger.domain.exception.DuplicateMessageException;
import com.polarishb.pabal.messenger.domain.model.Message;
import com.polarishb.pabal.messenger.domain.model.type.MessageStatus;
import com.polarishb.pabal.messenger.domain.model.type.MessageType;
import com.polarishb.pabal.support.AbstractPostgresDataJpaTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MessageWriteRepositoryImplTest extends AbstractPostgresDataJpaTest {

    @Autowired
    private MessageWriteRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID tenantId;
    private UUID chatRoomId;
    private UUID senderId;
    private UUID clientMessageId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        chatRoomId = UUID.randomUUID();
        senderId = UUID.randomUUID();
        clientMessageId = UUID.randomUUID();
        insertChatRoom();
    }

    @Test
    void append_translates_client_message_unique_violation_to_duplicate_message() {
        repository.append(draftMessage(1L));

        assertThatThrownBy(() -> repository.append(draftMessage(2L)))
                .isInstanceOf(DuplicateMessageException.class)
                .hasCauseInstanceOf(DataIntegrityViolationException.class);
    }

    private void insertChatRoom() {
        Instant now = Instant.parse("2026-04-02T12:00:00Z");
        Timestamp timestamp = Timestamp.from(now);

        jdbcTemplate.update("""
                        INSERT INTO chat_room (
                            id,
                            type,
                            created_by,
                            tenant_id,
                            is_private,
                            status,
                            last_message_sequence,
                            version,
                            created_at,
                            updated_at
                        )
                        VALUES (?, 'GROUP', ?, ?, false, 'ACTIVE', 0, 0, ?, ?)
                        """,
                chatRoomId,
                senderId,
                tenantId,
                timestamp,
                timestamp
        );
    }

    private PersistedMessage draftMessage(long sequence) {
        Instant now = Instant.parse("2026-04-02T12:00:00Z");
        MessageState state = new MessageState(
                null,
                tenantId,
                chatRoomId,
                senderId,
                clientMessageId,
                sequence,
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
