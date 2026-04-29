package com.polarishb.pabal.messenger.contract.persistence.chatroom;

import com.polarishb.pabal.messenger.domain.model.snapshot.ChatRoomSnapshot;
import com.polarishb.pabal.messenger.domain.model.type.RoomStatus;
import com.polarishb.pabal.messenger.domain.model.type.RoomType;
import com.polarishb.pabal.messenger.domain.model.vo.ChannelSettings;
import com.polarishb.pabal.messenger.domain.model.vo.RoomName;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ChatRoomState(
        ChatRoomSnapshot snapshot,
        Long version
) {
    public ChatRoomState {
        Objects.requireNonNull(snapshot);
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
            Long version
    ) {
        this(
                new ChatRoomSnapshot(
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
                        updatedAt
                ),
                version
        );
    }

    public UUID id() {
        return snapshot.id();
    }

    public RoomType type() {
        return snapshot.type();
    }

    public String name() {
        return snapshot.name().valueOrNull();
    }

    public UUID createdBy() {
        return snapshot.createdBy();
    }

    public UUID tenantId() {
        return snapshot.tenantId();
    }

    public ChannelSettings channelSettings() {
        return snapshot.channelSettings();
    }

    public RoomStatus status() {
        return snapshot.status();
    }

    public Instant scheduledDeletionAt() {
        return snapshot.scheduledDeletionAt();
    }

    public UUID lastMessageId() {
        return snapshot.lastMessageId();
    }

    public Long lastMessageSequence() {
        return snapshot.lastMessageSequence();
    }

    public Instant lastMessageAt() {
        return snapshot.lastMessageAt();
    }

    public Instant createdAt() {
        return snapshot.createdAt();
    }

    public Instant updatedAt() {
        return snapshot.updatedAt();
    }
}
