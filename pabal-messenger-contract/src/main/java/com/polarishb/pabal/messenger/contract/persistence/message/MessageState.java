package com.polarishb.pabal.messenger.contract.persistence.message;

import com.polarishb.pabal.messenger.domain.model.snapshot.MessageSnapshot;
import com.polarishb.pabal.messenger.domain.model.type.MessageStatus;
import com.polarishb.pabal.messenger.domain.model.type.MessageType;
import com.polarishb.pabal.messenger.domain.model.vo.MessageContent;

import java.time.Instant;
import java.util.UUID;

public record MessageState(
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
    public MessageState(
            MessageSnapshot snapshot,
            Long version
    ) {
        this(
                snapshot.id(),
                snapshot.tenantId(),
                snapshot.chatRoomId(),
                snapshot.senderId(),
                snapshot.clientMessageId(),
                snapshot.sequence(),
                snapshot.type(),
                snapshot.content().value(),
                snapshot.status(),
                snapshot.replyToMessageId(),
                snapshot.createdAt(),
                snapshot.updatedAt(),
                snapshot.deletedAt(),
                version
        );
    }

    public MessageSnapshot snapshot() {
        return new MessageSnapshot(
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
        );
    }
}
