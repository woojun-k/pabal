package com.polarishb.pabal.messenger.application.query.output;

import com.polarishb.pabal.messenger.domain.model.snapshot.MessageSnapshot;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record MessageDto(
        MessageSnapshot snapshot
) {
    public MessageDto {
        Objects.requireNonNull(snapshot);
    }

    public UUID messageId() {
        return snapshot.id();
    }

    public UUID chatRoomId() {
        return snapshot.chatRoomId();
    }

    public UUID senderId() {
        return snapshot.senderId();
    }

    public UUID clientMessageId() {
        return snapshot.clientMessageId();
    }

    public String content() {
        return snapshot.content().value();
    }

    public String status() {
        return snapshot.status().name();
    }

    public UUID replyToMessageId() {
        return snapshot.replyToMessageId();
    }

    public Instant createdAt() {
        return snapshot.createdAt();
    }

    public Instant updatedAt() {
        return snapshot.updatedAt();
    }

    public Instant deletedAt() {
        return snapshot.deletedAt();
    }
}
