package com.polarishb.pabal.messenger.application.query.output;

import java.time.Instant;
import java.util.UUID;

public record MessageDto(
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
