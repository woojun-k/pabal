package com.polarishb.pabal.messenger.contract.realtime;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record RoomEventEnvelope(
    UUID eventId,
    int schemaVersion,
    RoomEventType type,
    UUID tenantId,
    UUID chatRoomId,
    long sequence,
    Long aggregateVersion,
    Instant occurredAt,
    RoomEventPayload payload
) {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    public RoomEventEnvelope {
        Objects.requireNonNull(eventId);
        Objects.requireNonNull(type);
        Objects.requireNonNull(tenantId);
        Objects.requireNonNull(chatRoomId);
        Objects.requireNonNull(occurredAt);
        Objects.requireNonNull(payload);
        if (schemaVersion < 1) {
            throw new IllegalArgumentException("schemaVersion must be positive");
        }
        if (sequence < 0) {
            throw new IllegalArgumentException("sequence must not be negative");
        }
    }

    public static RoomEventEnvelope of(
            RoomEventType type,
            UUID tenantId,
            UUID chatRoomId,
            long sequence,
            Long aggregateVersion,
            Instant occurredAt,
            RoomEventPayload payload
    ) {
        return new RoomEventEnvelope(
                UUID.randomUUID(),
                CURRENT_SCHEMA_VERSION,
                type,
                tenantId,
                chatRoomId,
                sequence,
                aggregateVersion,
                occurredAt,
                payload
        );
    }
}
