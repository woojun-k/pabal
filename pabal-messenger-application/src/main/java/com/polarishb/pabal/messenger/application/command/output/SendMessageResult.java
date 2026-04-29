package com.polarishb.pabal.messenger.application.command.output;

import java.time.Instant;
import java.util.UUID;

public record SendMessageResult(
    UUID messageId,
    UUID clientMessageId,
    Instant createdAt,
    boolean isDuplicated
) {}