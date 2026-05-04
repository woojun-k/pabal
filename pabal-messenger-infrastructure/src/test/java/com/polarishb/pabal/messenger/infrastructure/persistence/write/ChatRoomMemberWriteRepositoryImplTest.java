package com.polarishb.pabal.messenger.infrastructure.persistence.write;

import com.polarishb.pabal.messenger.application.port.out.persistence.ChatRoomMemberWriteRepository;
import com.polarishb.pabal.messenger.contract.persistence.chatroommember.ChatRoomMemberPersistenceMapper;
import com.polarishb.pabal.messenger.contract.persistence.chatroommember.ChatRoomMemberState;
import com.polarishb.pabal.messenger.contract.persistence.chatroommember.PersistedChatRoomMember;
import com.polarishb.pabal.messenger.domain.model.ChatRoomMember;
import com.polarishb.pabal.messenger.domain.model.type.RoomStatus;
import com.polarishb.pabal.messenger.domain.model.type.RoomType;
import com.polarishb.pabal.support.AbstractPostgresDataJpaTest;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChatRoomMemberWriteRepositoryImplTest extends AbstractPostgresDataJpaTest {

    @Autowired
    private ChatRoomMemberWriteRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID tenantId;
    private UUID chatRoomId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        chatRoomId = UUID.randomUUID();
        userId = UUID.randomUUID();
        insertChatRoom();
    }

    @Test
    void update_rejects_state_with_matching_id_but_wrong_tenant() {
        Instant joinedAt = Instant.parse("2026-04-02T12:00:00Z");
        PersistedChatRoomMember saved = repository.append(draftMember(tenantId, joinedAt));
        UUID wrongTenantId = UUID.randomUUID();

        ChatRoomMember wrongTenantMember = ChatRoomMember.reconstitute(
                saved.state().id(),
                wrongTenantId,
                chatRoomId,
                userId,
                null,
                saved.state().lastReadSequence(),
                null,
                joinedAt,
                null,
                joinedAt,
                joinedAt
        );
        ChatRoomMemberState wrongTenantState = new ChatRoomMemberState(
                saved.state().id(),
                wrongTenantId,
                chatRoomId,
                userId,
                null,
                saved.state().lastReadSequence(),
                null,
                joinedAt,
                null,
                joinedAt,
                joinedAt,
                saved.state().version()
        );

        assertThatThrownBy(() -> repository.update(new PersistedChatRoomMember(wrongTenantMember, wrongTenantState)))
                .isInstanceOf(EntityNotFoundException.class);
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
                RoomType.GROUP.name(),
                userId,
                tenantId,
                RoomStatus.ACTIVE.name(),
                timestamp,
                timestamp
        );
    }

    private PersistedChatRoomMember draftMember(UUID memberTenantId, Instant joinedAt) {
        ChatRoomMember member = ChatRoomMember.join(
                memberTenantId,
                chatRoomId,
                userId,
                joinedAt,
                0L
        );
        ChatRoomMemberState state = ChatRoomMemberPersistenceMapper.toState(member, null);
        return new PersistedChatRoomMember(member, state);
    }
}
