package com.polarishb.pabal.messenger.application.command.output;

import java.time.Instant;
import java.util.UUID;

public record EditMessageResult(
    UUID messageId,
    String content,
    Instant updatedAt
) {}