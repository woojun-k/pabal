package com.polarishb.pabal.messenger.domain.event;

import com.polarishb.pabal.common.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record MessageDeletedEvent(
    UUID tenantId,
    UUID messageId,
    UUID chatRoomId,
    UUID senderId,
    long sequence,
    Instant occurredAt,
    Long aggregateVersion
) implements DomainEvent {}
