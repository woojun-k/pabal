package com.polarishb.pabal.messenger.api.command.http.response;

import java.time.Instant;
import java.util.UUID;

public record SendMessageResponse(
        UUID messageId,
        UUID clientMessageId,
        Instant createdAt,
        boolean duplicated
) {
}
