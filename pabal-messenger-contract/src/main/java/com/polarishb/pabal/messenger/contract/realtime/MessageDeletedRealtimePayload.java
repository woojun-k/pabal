package com.polarishb.pabal.messenger.contract.realtime;

import java.time.Instant;
import java.util.UUID;

public record MessageDeletedRealtimePayload(
    UUID messageId,
    UUID chatRoomId,
    long sequence,
    Instant deletedAt
) implements RoomEventPayload {}
