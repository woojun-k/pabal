package com.polarishb.pabal.messenger.domain.model.snapshot;

import com.polarishb.pabal.messenger.domain.model.type.MessageStatus;
import com.polarishb.pabal.messenger.domain.model.type.MessageType;
import com.polarishb.pabal.messenger.domain.model.vo.MessageContent;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record MessageSnapshot(
        UUID id,
        UUID tenantId,
        UUID chatRoomId,
        UUID senderId,
        UUID clientMessageId,
        Long sequence,
        MessageType type,
        MessageContent content,
        MessageStatus status,
        UUID replyToMessageId,
        Instant createdAt,
        Instant updatedAt,
        Instant deletedAt
) {
    public MessageSnapshot {
        Objects.requireNonNull(tenantId);
        Objects.requireNonNull(chatRoomId);
        Objects.requireNonNull(senderId);
        Objects.requireNonNull(clientMessageId);
        Objects.requireNonNull(type);
        Objects.requireNonNull(content);
        Objects.requireNonNull(status);
        Objects.requireNonNull(createdAt);
        Objects.requireNonNull(updatedAt);
    }
}
