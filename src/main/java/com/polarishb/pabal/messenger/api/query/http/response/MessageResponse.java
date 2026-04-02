package com.polarishb.pabal.messenger.api.query.http.response;

import java.time.Instant;
import java.util.UUID;

public record MessageResponse(
        UUID messageId,
        UUID chatRoomId,
        UUID senderId,
        UUID clientMessageId,
        String content,
        String status,
        UUID replyToMessageId,
        Instant createdAt,
        Instant updatedAt,
        Instant deletedAt
) {
}
