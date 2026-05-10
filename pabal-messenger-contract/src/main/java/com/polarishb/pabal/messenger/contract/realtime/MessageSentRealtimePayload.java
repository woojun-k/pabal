package com.polarishb.pabal.messenger.contract.realtime;

import java.time.Instant;
import java.util.UUID;

public record MessageSentRealtimePayload(
    UUID messageId,
    UUID chatRoomId,
    long sequence,
    UUID senderId,
    UUID clientMessageId,
    String content,
    Instant createdAt
) implements RoomEventPayload {}
