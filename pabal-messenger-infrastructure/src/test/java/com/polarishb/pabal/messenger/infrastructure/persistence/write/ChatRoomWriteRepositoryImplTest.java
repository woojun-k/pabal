package com.polarishb.pabal.messenger.infrastructure.persistence.write;

import com.polarishb.pabal.messenger.application.port.out.persistence.ChatRoomWriteRepository;
import com.polarishb.pabal.messenger.contract.persistence.chatroom.ChatRoomPersistenceMapper;
import com.polarishb.pabal.messenger.contract.persistence.chatroom.ChatRoomState;
import com.polarishb.pabal.messenger.contract.persistence.chatroom.PersistedChatRoom;
import com.polarishb.pabal.messenger.domain.model.ChatRoom;
import com.polarishb.pabal.messenger.domain.model.type.RoomStatus;
import com.polarishb.pabal.messenger.domain.model.type.RoomType;
import com.polarishb.pabal.messenger.domain.model.vo.ChannelName;
import com.polarishb.pabal.messenger.infrastructure.persistence.jpa.write.ChatRoomWriteJpaRepository;
import com.polarishb.pabal.support.AbstractPostgresDataJpaTest;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChatRoomWriteRepositoryImplTest extends AbstractPostgresDataJpaTest {

    @Autowired
    private ChatRoomWriteRepository repository;

    @Autowired
    private ChatRoomWriteJpaRepository jpaRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void update_persists_deleted_at_when_channel_is_deleted_immediately() {
        UUID tenantId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-04-02T12:00:00Z");
        Instant scheduledAt = createdAt.plusSeconds(60);
        Instant deletedAt = createdAt.plusSeconds(120);

        ChatRoom room = ChatRoom.createChannel(
                "general",
                ownerId,
                tenantId,
                UUID.randomUUID(),
                false,
                "team room",
                createdAt
        );
        room.scheduleForDeletion(scheduledAt);

        PersistedChatRoom saved = repository.append(draft(room));
        saved.chatRoom().deleteImmediately(deletedAt);
        PersistedChatRoom deleted = repository.update(saved);
        jpaRepository.flush();

        assertThat(deleted.state().status()).isEqualTo(RoomStatus.DELETED);
        assertThat(deleted.state().deletedAt()).isEqualTo(deletedAt);

        Timestamp storedDeletedAt = jdbcTemplate.queryForObject(
                "SELECT deleted_at FROM chat_room WHERE id = ?",
                Timestamp.class,
                deleted.state().id()
        );
        String storedStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM chat_room WHERE id = ?",
                String.class,
                deleted.state().id()
        );

        assertThat(storedStatus).isEqualTo(RoomStatus.DELETED.name());
        assertThat(storedDeletedAt).isNotNull();
        assertThat(storedDeletedAt.toInstant()).isEqualTo(deletedAt);
    }

    @Test
    void update_rejects_state_with_matching_id_but_wrong_tenant() {
        UUID tenantId = UUID.randomUUID();
        UUID wrongTenantId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-04-02T12:00:00Z");

        ChatRoom room = ChatRoom.createChannel(
                "tenantroom",
                ownerId,
                tenantId,
                UUID.randomUUID(),
                false,
                null,
                createdAt
        );
        PersistedChatRoom saved = repository.append(draft(room));

        ChatRoom wrongTenantRoom = ChatRoom.reconstitute(
                saved.state().id(),
                RoomType.CHANNEL,
                new ChannelName("tenantroom"),
                ownerId,
                wrongTenantId,
                saved.state().channelSettings(),
                RoomStatus.ACTIVE,
                null,
                null,
                saved.state().lastMessageSequence(),
                null,
                createdAt,
                createdAt
        );
        ChatRoomState wrongTenantState = new ChatRoomState(
                saved.state().id(),
                RoomType.CHANNEL,
                wrongTenantRoom.getName().valueOrNull(),
                ownerId,
                wrongTenantId,
                saved.state().channelSettings(),
                RoomStatus.ACTIVE,
                null,
                null,
                saved.state().lastMessageSequence(),
                null,
                createdAt,
                createdAt,
                saved.state().version()
        );

        assertThatThrownBy(() -> repository.update(new PersistedChatRoom(wrongTenantRoom, wrongTenantState)))
                .isInstanceOf(EntityNotFoundException.class);
    }

    private static PersistedChatRoom draft(ChatRoom room) {
        ChatRoomState state = ChatRoomPersistenceMapper.toState(room, null);
        return new PersistedChatRoom(room, state);
    }
}
