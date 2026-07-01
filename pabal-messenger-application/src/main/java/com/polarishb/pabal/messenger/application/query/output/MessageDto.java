package com.polarishb.pabal.messenger.application.query.output;

import java.time.Instant;
import java.util.UUID;

public record MessageDto(
        UUID messageId,
        UUID chatRoomId,
        UUID senderId,
        UUID clientMessageId,
        Long sequence,
        String content,
        String status,
        UUID replyToMessageId,
        Instant createdAt,
        Instant updatedAt,
        Instant deletedAt
) {
    private static final String DELETED_STATUS = "DELETED";
    private static final String DELETED_TOMBSTONE_VALUE = "[deleted]";

    public String content() {
        if (DELETED_STATUS.equals(status)) {
            return DELETED_TOMBSTONE_VALUE;
        }
        return content;
    }
}
