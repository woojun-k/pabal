package com.polarishb.pabal.messenger.application.query.output;

import java.time.Instant;
import java.util.UUID;

public record RoomDto(
        UUID roomId,
        String name,
        String type,
        String status,
        UUID lastMessageId,
        Instant lastMessageAt,
        long unreadCount,
        Instant joinedAt
) {
}
