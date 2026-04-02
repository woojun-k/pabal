package com.polarishb.pabal.messenger.api.query.http.response;

import java.time.Instant;
import java.util.UUID;

public record RoomResponse(
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
