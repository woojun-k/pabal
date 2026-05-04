package com.polarishb.pabal.messenger.api.command.http.response;

import java.time.Instant;
import java.util.UUID;

public record EditMessageResponse(
    UUID messageId,
    long sequence,
    String content,
    Instant updatedAt
) {}
