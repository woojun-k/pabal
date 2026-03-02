package com.polarishb.pabal.messenger.domain.repository.result;

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
