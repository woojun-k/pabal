package com.polarishb.pabal.messenger.contract.realtime;

import java.time.Instant;
import java.util.UUID;

public record TypingEventPayload(
    UUID userId,
    String status,
    Instant occurredAt
) {
    public static TypingEventPayload started(UUID userId, Instant occurredAt) {
        return new TypingEventPayload(userId, "STARTED", occurredAt);
    }

    public static TypingEventPayload stopped(UUID userId, Instant occurredAt) {
        return new TypingEventPayload(userId, "STOPPED", occurredAt);
    }
}
