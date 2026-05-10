package com.polarishb.pabal.messenger.contract.persistence.message;

import com.polarishb.pabal.messenger.domain.model.snapshot.MessageSnapshot;
import com.polarishb.pabal.messenger.domain.model.type.MessageStatus;
import com.polarishb.pabal.messenger.domain.model.type.MessageType;
import com.polarishb.pabal.messenger.domain.model.vo.MessageContent;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record MessageState(
        MessageSnapshot snapshot,
        Long version
) {
    public MessageState {
        Objects.requireNonNull(snapshot);
        Objects.requireNonNull(snapshot.sequence(), "message.sequence must be assigned before persistence");
    }

    public MessageState(
            UUID id,
            UUID tenantId,
            UUID chatRoomId,
            UUID senderId,
            UUID clientMessageId,
            Long sequence,
            MessageType type,
            String content,
            MessageStatus status,
            UUID replyToMessageId,
            Instant createdAt,
            Instant updatedAt,
            Instant deletedAt,
            Long version
    ) {
        this(
                new MessageSnapshot(
                        id,
                        tenantId,
                        chatRoomId,
                        senderId,
                        clientMessageId,
                        sequence,
                        type,
                        new MessageContent(content),
                        status,
                        replyToMessageId,
                        createdAt,
                        updatedAt,
                        deletedAt
                ),
                version
        );
    }

    public UUID id() {
        return snapshot.id();
    }

    public UUID tenantId() {
        return snapshot.tenantId();
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

    public Long sequence() {
        return snapshot.sequence();
    }

    public MessageType type() {
        return snapshot.type();
    }

    public String content() {
        return snapshot.content().value();
    }

    public MessageStatus status() {
        return snapshot.status();
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
