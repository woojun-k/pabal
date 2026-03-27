package com.polarishb.pabal.messenger.contract.realtime;

import com.polarishb.pabal.messenger.domain.model.type.TypingStatus;

import java.time.Instant;
import java.util.UUID;

public record TypingEventPayload(
    UUID userId,
    TypingStatus status,
    Instant occurredAt
) {
    public static TypingEventPayload started(UUID userId, Instant occurredAt) {
        return new TypingEventPayload(userId, TypingStatus.STARTED, occurredAt);
    }

    public static TypingEventPayload stopped(UUID userId, Instant occurredAt) {
        return new TypingEventPayload(userId, TypingStatus.STOPPED, occurredAt);
    }
}
