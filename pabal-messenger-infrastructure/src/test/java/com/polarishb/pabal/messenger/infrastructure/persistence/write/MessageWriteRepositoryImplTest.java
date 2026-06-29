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
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MessageWriteRepositoryImplTest extends AbstractPostgresDataJpaTest {

    @Autowired
    private MessageWriteRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

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

    @Test
    void concurrent_append_with_same_client_message_id_translates_database_unique_race_to_duplicate_message()
            throws Exception {
        TestTransaction.flagForCommit();
        TestTransaction.end();

        CyclicBarrier startBarrier = new CyclicBarrier(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Throwable> first = executor.submit(() -> appendInTransactionAfterBarrier(startBarrier, 1L));
            Future<Throwable> second = executor.submit(() -> appendInTransactionAfterBarrier(startBarrier, 2L));

            List<Throwable> results = new ArrayList<>();
            results.add(first.get(10, TimeUnit.SECONDS));
            results.add(second.get(10, TimeUnit.SECONDS));
            List<Throwable> failures = results.stream()
                    .filter(Throwable.class::isInstance)
                    .toList();

            assertThat(failures).hasSize(1);
            assertThat(failures.getFirst())
                    .isInstanceOf(DuplicateMessageException.class)
                    .hasCauseInstanceOf(DataIntegrityViolationException.class);
            assertThat(countMessages()).isEqualTo(1);
        } finally {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    private Throwable appendInTransactionAfterBarrier(CyclicBarrier startBarrier, long sequence) {
        try {
            startBarrier.await(5, TimeUnit.SECONDS);
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> repository.append(draftMessage(sequence)));
            return null;
        } catch (Throwable throwable) {
            return unwrapExecutionException(throwable);
        }
    }

    private Throwable unwrapExecutionException(Throwable throwable) {
        if (throwable instanceof ExecutionException executionException && executionException.getCause() != null) {
            return executionException.getCause();
        }
        return throwable;
    }

    private long countMessages() {
        Long count = jdbcTemplate.queryForObject("""
                        SELECT count(*)
                        FROM message
                        WHERE tenant_id = ?
                          AND chat_room_id = ?
                          AND sender_id = ?
                          AND client_message_id = ?
                        """,
                Long.class,
                tenantId,
                chatRoomId,
                senderId,
                clientMessageId
        );
        return count != null ? count : 0L;
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
