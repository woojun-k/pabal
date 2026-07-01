package com.polarishb.pabal.messenger.contract.persistence.chatroom;

import com.polarishb.pabal.messenger.domain.model.snapshot.ChatRoomSnapshot;
import com.polarishb.pabal.messenger.domain.model.type.RoomStatus;
import com.polarishb.pabal.messenger.domain.model.type.RoomType;
import com.polarishb.pabal.messenger.domain.model.vo.ChannelSettings;
import com.polarishb.pabal.messenger.domain.model.vo.RoomName;

import java.time.Instant;
import java.util.UUID;

public record ChatRoomState(
        UUID id,
        RoomType type,
        String name,
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
        Instant deletedAt,
        Long version
) {
    public ChatRoomState(
            UUID id,
            RoomType type,
            String name,
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
            Long version
    ) {
        this(
                id,
                type,
                name,
                createdBy,
                tenantId,
                channelSettings,
                status,
                scheduledDeletionAt,
                lastMessageId,
                lastMessageSequence,
                lastMessageAt,
                createdAt,
                updatedAt,
                null,
                version
        );
    }

    public ChatRoomState(
            ChatRoomSnapshot snapshot,
            Long version
    ) {
        this(
                snapshot.id(),
                snapshot.type(),
                snapshot.name().valueOrNull(),
                snapshot.createdBy(),
                snapshot.tenantId(),
                snapshot.channelSettings(),
                snapshot.status(),
                snapshot.scheduledDeletionAt(),
                snapshot.lastMessageId(),
                snapshot.lastMessageSequence(),
                snapshot.lastMessageAt(),
                snapshot.createdAt(),
                snapshot.updatedAt(),
                snapshot.deletedAt(),
                version
        );
    }

    public ChatRoomState(
            UUID id,
            RoomType type,
            String name,
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
            Instant deletedAt,
            Long version
    ) {
        this.id = id;
        this.type = type;
        this.name = name;
        this.createdBy = createdBy;
        this.tenantId = tenantId;
        this.channelSettings = channelSettings;
        this.status = status;
        this.scheduledDeletionAt = scheduledDeletionAt;
        this.lastMessageId = lastMessageId;
        this.lastMessageSequence = lastMessageSequence;
        this.lastMessageAt = lastMessageAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
        this.version = version;
    }

    public ChatRoomSnapshot snapshot() {
        return new ChatRoomSnapshot(
                id,
                type,
                RoomName.of(type, name),
                createdBy,
                tenantId,
                channelSettings,
                status,
                scheduledDeletionAt,
                lastMessageId,
                lastMessageSequence,
                lastMessageAt,
                createdAt,
                updatedAt,
                deletedAt
        );
    }
}
