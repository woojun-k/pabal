package com.polarishb.pabal.messenger.domain.model.entity;

import com.polarishb.pabal.messenger.domain.exception.RoomMustBePendingDeletionException;
import com.polarishb.pabal.messenger.domain.model.type.RoomStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChatRoomTest {

    @Test
    void deleteImmediately_requires_pending_deletion_status() {
        Instant createdAt = Instant.parse("2026-04-02T00:00:00Z");
        ChatRoom room = ChatRoom.createChannel(
                "general",
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                false,
                "team room",
                createdAt
        );

        assertThatThrownBy(() -> room.deleteImmediately(createdAt.plusSeconds(60)))
                .isInstanceOf(RoomMustBePendingDeletionException.class);
    }

    @Test
    void deleteImmediately_marks_pending_channel_deleted_with_application_time() {
        Instant createdAt = Instant.parse("2026-04-02T00:00:00Z");
        Instant scheduledAt = createdAt.plusSeconds(60);
        Instant deletedAt = createdAt.plusSeconds(120);

        ChatRoom room = ChatRoom.createChannel(
                "general",
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                false,
                "team room",
                createdAt
        );
        room.scheduleForDeletion(scheduledAt);

        room.deleteImmediately(deletedAt);

        assertThat(room.getStatus()).isEqualTo(RoomStatus.DELETED);
        assertThat(room.getScheduledDeletionAt()).isNull();
        assertThat(room.getUpdatedAt()).isEqualTo(deletedAt);
    }
}
