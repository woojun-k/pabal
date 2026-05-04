package com.polarishb.pabal.messenger.domain.event;

import com.polarishb.pabal.common.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record MessageReadEvent(
    UUID tenantId,
    UUID chatRoomId,
    UUID userId,
    UUID lastReadMessageId,
    long sequence,
    Instant readAt,
    Long aggregateVersion
) implements DomainEvent {}
