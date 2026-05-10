package com.polarishb.pabal.messenger.domain.model.snapshot;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record DirectChatMappingSnapshot(
        UUID id,
        UUID tenantId,
        UUID chatRoomId,
        UUID userIdMin,
        UUID userIdMax,
        Instant createdAt,
        Instant updatedAt
) {
    public DirectChatMappingSnapshot {
        Objects.requireNonNull(tenantId);
        Objects.requireNonNull(chatRoomId);
        Objects.requireNonNull(userIdMin);
        Objects.requireNonNull(userIdMax);
        Objects.requireNonNull(createdAt);
        Objects.requireNonNull(updatedAt);
    }
}
