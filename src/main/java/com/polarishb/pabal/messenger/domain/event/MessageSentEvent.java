package com.polarishb.pabal.messenger.domain.event;

import com.polarishb.pabal.common.event.DomainEvent;

import java.util.UUID;

public record MessageSentEvent(
    UUID tenantId,
    UUID messageId,
    UUID chatRoomId,
    UUID senderId
) implements DomainEvent {}