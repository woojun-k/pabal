package com.polarishb.pabal.messenger.api.command.http.response;

import java.time.Instant;
import java.util.UUID;

public record DeleteMessageResponse(
    UUID messageId,
    Instant deletedAt
) {}
