package com.polarishb.pabal.messenger.domain.event;

import com.polarishb.pabal.common.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record MessageSentEvent(
    UUID tenantId,
    UUID messageId,
    UUID chatRoomId,
    UUID senderId,
    UUID clientMessageId,
    long sequence,
    String content,
    Instant occurredAt,
    Long aggregateVersion
) implements DomainEvent {}
