package com.polarishb.pabal.messenger.contract.realtime;

import java.time.Instant;
import java.util.UUID;

public record MessageEditedRealtimePayload(
    UUID messageId,
    UUID chatRoomId,
    long sequence,
    String content,
    Instant updatedAt
) implements RoomEventPayload {}
