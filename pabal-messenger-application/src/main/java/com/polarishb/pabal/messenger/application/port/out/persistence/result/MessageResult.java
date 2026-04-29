package com.polarishb.pabal.messenger.application.port.out.persistence.result;

import java.time.Instant;
import java.util.UUID;

public record MessageResult(
    UUID id,
    UUID tenantId,
    UUID chatRoomId,
    UUID senderId,
    UUID clientMessageId,
    Instant createdAt
) {}
