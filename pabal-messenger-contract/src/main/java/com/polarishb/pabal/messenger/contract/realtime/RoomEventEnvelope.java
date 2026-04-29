package com.polarishb.pabal.messenger.contract.realtime;

import java.time.Instant;

public record RoomEventEnvelope(
    RoomEventType type,
    Object payload,
    Instant occurredAt
) {
    public static RoomEventEnvelope of(RoomEventType type, Object payload, Instant occurredAt) {
        return new RoomEventEnvelope(type, payload, occurredAt);
    }
}
