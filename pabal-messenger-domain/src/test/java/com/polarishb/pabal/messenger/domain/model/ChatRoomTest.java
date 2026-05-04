package com.polarishb.pabal.messenger.domain.model;

import com.polarishb.pabal.messenger.domain.exception.InvalidRoomStatusTransitionException;
import com.polarishb.pabal.messenger.domain.exception.RoomJoinForbiddenException;
import com.polarishb.pabal.messenger.domain.exception.RoomMustBePendingDeletionException;
import com.polarishb.pabal.messenger.domain.exception.RoomOperationNotAllowedException;
import com.polarishb.pabal.messenger.domain.model.snapshot.ChatRoomSnapshot;
import com.polarishb.pabal.messenger.domain.model.type.RoomStatus;
import com.polarishb.pabal.messenger.domain.model.type.RoomType;
import com.polarishb.pabal.messenger.domain.model.vo.ChannelName;
import com.polarishb.pabal.messenger.domain.model.vo.ChannelSettings;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChatRoomTest {

    @Test
    void snapshot_round_trip_preserves_channel_room_state() {
        UUID roomId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        UUID lastMessageId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-04-02T00:00:00Z");
        Instant updatedAt = createdAt.plusSeconds(60);
        Instant scheduledDeletionAt = createdAt.plusSeconds(3600);
        Instant lastMessageAt = createdAt.plusSeconds(30);

        ChatRoom room = ChatRoom.reconstitute(
                roomId,
                RoomType.CHANNEL,
                new ChannelName("General"),
                createdBy,
                tenantId,
                new ChannelSettings(workspaceId, true, "team room"),
                RoomStatus.PENDING_DELETION,
                scheduledDeletionAt,
                lastMessageId,
                42L,
                lastMessageAt,
                createdAt,
                updatedAt
        );

        ChatRoomSnapshot snapshot = room.snapshot();
        ChatRoom restored = ChatRoom.reconstitute(snapshot);

        assertThat(restored.snapshot()).isEqualTo(snapshot);
        assertThat(snapshot.name().valueOrNull()).isEqualTo("general");
    }

    @Test
    void snapshot_rejects_deleted_room_without_deleted_at() {
        Instant createdAt = Instant.parse("2026-04-02T00:00:00Z");

        assertThatThrownBy(() -> new ChatRoomSnapshot(
                UUID.randomUUID(),
                RoomType.CHANNEL,
                new ChannelName("general"),
                UUID.randomUUID(),
                UUID.randomUUID(),
                new ChannelSettings(UUID.randomUUID(), false, null),
                RoomStatus.DELETED,
                null,
                null,
                0L,
                null,
                createdAt,
                createdAt,
                null
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void public_active_channel_allows_self_join() {
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

        assertThat(room.canJoin()).isTrue();
        assertThatCode(room::validateCanJoin).doesNotThrowAnyException();
    }

    @Test
    void private_active_channel_rejects_room_id_only_self_join() {
        Instant createdAt = Instant.parse("2026-04-02T00:00:00Z");
        ChatRoom room = ChatRoom.createChannel(
                "secret",
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                true,
                "private room",
                createdAt
        );

        assertThat(room.canJoin()).isFalse();
        assertThatThrownBy(room::validateCanJoin)
                .isInstanceOf(RoomJoinForbiddenException.class);
    }

    @Test
    void direct_and_group_rooms_reject_room_id_only_self_join() {
        Instant createdAt = Instant.parse("2026-04-02T00:00:00Z");
        ChatRoom directRoom = ChatRoom.createDirect(null, UUID.randomUUID(), UUID.randomUUID(), createdAt);
        ChatRoom groupRoom = ChatRoom.createGroup("team", UUID.randomUUID(), UUID.randomUUID(), createdAt);

        assertThat(directRoom.canJoin()).isFalse();
        assertThat(groupRoom.canJoin()).isFalse();
        assertThatThrownBy(directRoom::validateCanJoin)
                .isInstanceOf(RoomJoinForbiddenException.class);
        assertThatThrownBy(groupRoom::validateCanJoin)
                .isInstanceOf(RoomJoinForbiddenException.class);
    }

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
        assertThat(room.getDeletedAt()).isEqualTo(deletedAt);
    }

    @Test
    void pending_deletion_room_blocks_send_read_subscribe_and_join() {
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

        room.scheduleForDeletion(createdAt.plusSeconds(60));

        assertThat(room.canSend()).isFalse();
        assertThat(room.canRead()).isFalse();
        assertThat(room.canSubscribe()).isFalse();
        assertThat(room.canJoin()).isFalse();

        assertThatThrownBy(room::validateCanSend).isInstanceOf(RoomOperationNotAllowedException.class);
        assertThatThrownBy(room::validateCanRead).isInstanceOf(RoomOperationNotAllowedException.class);
        assertThatThrownBy(room::validateCanSubscribe).isInstanceOf(RoomOperationNotAllowedException.class);
        assertThatThrownBy(room::validateCanJoin).isInstanceOf(RoomOperationNotAllowedException.class);
    }

    @Test
    void updateLastMessage_keeps_highest_sequence() {
        Instant createdAt = Instant.parse("2026-04-02T00:00:00Z");
        ChatRoom room = ChatRoom.createGroup("team", UUID.randomUUID(), UUID.randomUUID(), createdAt);

        UUID firstMessageId = UUID.randomUUID();
        UUID secondMessageId = UUID.randomUUID();

        room.updateLastMessage(firstMessageId, 10L, createdAt.plusSeconds(10));
        room.updateLastMessage(secondMessageId, 9L, createdAt.plusSeconds(20));

        assertThat(room.getLastMessageId()).isEqualTo(firstMessageId);
        assertThat(room.getLastMessageSequence()).isEqualTo(10L);
        assertThat(room.getLastMessageAt()).isEqualTo(createdAt.plusSeconds(10));
    }

    @Test
    void scheduleForDeletion_rejects_deleted_room() {
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

        room.scheduleForDeletion(createdAt.plusSeconds(60));
        room.deleteImmediately(createdAt.plusSeconds(120));

        assertThatThrownBy(() -> room.scheduleForDeletion(createdAt.plusSeconds(180)))
                .isInstanceOf(InvalidRoomStatusTransitionException.class);

        assertThat(room.getStatus()).isEqualTo(RoomStatus.DELETED);
        assertThat(room.getScheduledDeletionAt()).isNull();
    }

    @Test
    void scheduleForDeletion_rejects_pending_deletion_room() {
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

        room.scheduleForDeletion(createdAt.plusSeconds(60));

        assertThatThrownBy(() -> room.scheduleForDeletion(createdAt.plusSeconds(120)))
                .isInstanceOf(InvalidRoomStatusTransitionException.class);

        assertThat(room.getStatus()).isEqualTo(RoomStatus.PENDING_DELETION);
    }
}
