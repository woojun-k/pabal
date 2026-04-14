package com.polarishb.pabal.messenger.contract.persistence.message;

import com.polarishb.pabal.messenger.domain.model.type.MessageStatus;
import com.polarishb.pabal.messenger.domain.model.type.MessageType;

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
) {}
