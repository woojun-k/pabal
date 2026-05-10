package com.polarishb.pabal.messenger.domain.event;

import com.polarishb.pabal.common.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record MemberJoinedEvent(
    UUID tenantId,
    UUID chatRoomId,
    UUID userId,
    long sequence,
    Instant joinedAt,
    Long aggregateVersion
) implements DomainEvent {}
