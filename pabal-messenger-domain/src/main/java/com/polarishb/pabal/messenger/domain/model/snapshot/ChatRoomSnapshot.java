package com.polarishb.pabal.messenger.domain.model.snapshot;

import com.polarishb.pabal.messenger.domain.model.type.RoomStatus;
import com.polarishb.pabal.messenger.domain.model.type.RoomType;
import com.polarishb.pabal.messenger.domain.model.vo.ChannelSettings;
import com.polarishb.pabal.messenger.domain.model.vo.RoomName;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ChatRoomSnapshot(
        UUID id,
        RoomType type,
        RoomName name,
        UUID createdBy,
        UUID tenantId,
        ChannelSettings channelSettings,
        RoomStatus status,
        Instant scheduledDeletionAt,
        UUID lastMessageId,
        Long lastMessageSequence,
        Instant lastMessageAt,
        Instant createdAt,
        Instant updatedAt,
        Instant deletedAt
) {
    public ChatRoomSnapshot {
        Objects.requireNonNull(type);
        Objects.requireNonNull(name);
        Objects.requireNonNull(createdBy);
        Objects.requireNonNull(tenantId);
        Objects.requireNonNull(status);
        Objects.requireNonNull(createdAt);
        Objects.requireNonNull(updatedAt);
        if (status == RoomStatus.DELETED && deletedAt == null) {
            throw new IllegalArgumentException("deletedAt is required when room status is DELETED");
        }
        if (status != RoomStatus.DELETED && deletedAt != null) {
            throw new IllegalArgumentException("deletedAt must be null when room status is not DELETED");
        }
    }
}
