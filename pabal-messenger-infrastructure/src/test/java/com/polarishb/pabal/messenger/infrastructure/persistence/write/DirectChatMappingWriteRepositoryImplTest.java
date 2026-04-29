package com.polarishb.pabal.messenger.infrastructure.persistence.write;

import com.polarishb.pabal.messenger.application.port.out.persistence.DirectChatMappingWriteRepository;
import com.polarishb.pabal.messenger.contract.persistence.directchatmapping.DirectChatMappingState;
import com.polarishb.pabal.messenger.contract.persistence.directchatmapping.PersistedDirectChatMapping;
import com.polarishb.pabal.messenger.domain.exception.DuplicateDirectChatMappingException;
import com.polarishb.pabal.messenger.domain.model.DirectChatMapping;
import com.polarishb.pabal.messenger.domain.model.type.RoomStatus;
import com.polarishb.pabal.messenger.domain.model.type.RoomType;
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

class DirectChatMappingWriteRepositoryImplTest extends AbstractPostgresDataJpaTest {

    @Autowired
    private DirectChatMappingWriteRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID tenantId;
    private UUID chatRoomId;
    private UUID userId1;
    private UUID userId2;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        chatRoomId = UUID.randomUUID();
        userId1 = UUID.randomUUID();
        userId2 = UUID.randomUUID();
        insertChatRoom();
    }

    @Test
    void flush_translates_unique_violation_to_domain_exception() {
        repository.append(draftMapping());
        repository.append(draftMapping());

        assertThatThrownBy(repository::flush)
                .isInstanceOf(DuplicateDirectChatMappingException.class)
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
                        VALUES (?, ?, ?, ?, false, ?, 0, 0, ?, ?)
                        """,
                chatRoomId,
                RoomType.DIRECT.name(),
                userId1,
                tenantId,
                RoomStatus.ACTIVE.name(),
                timestamp,
                timestamp
        );
    }

    private PersistedDirectChatMapping draftMapping() {
        UUID mappingId = UUID.randomUUID();
        Instant now = Instant.parse("2026-04-02T12:00:00Z");

        DirectChatMapping mapping = DirectChatMapping.reconstitute(
                mappingId,
                tenantId,
                chatRoomId,
                userId1.compareTo(userId2) < 0 ? userId1 : userId2,
                userId1.compareTo(userId2) < 0 ? userId2 : userId1,
                now,
                now
        );

        return new PersistedDirectChatMapping(
                mapping,
                new DirectChatMappingState(
                        mappingId,
                        tenantId,
                        chatRoomId,
                        mapping.getUserIdMin(),
                        mapping.getUserIdMax(),
                        now,
                        now,
                        null
                )
        );
    }
}
