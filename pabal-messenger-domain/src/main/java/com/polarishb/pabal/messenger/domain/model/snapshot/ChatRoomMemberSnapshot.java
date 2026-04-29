package com.polarishb.pabal.messenger.domain.model.snapshot;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ChatRoomMemberSnapshot(
        UUID id,
        UUID tenantId,
        UUID chatRoomId,
        UUID userId,
        UUID lastReadMessageId,
        Long lastReadSequence,
        Instant lastReadAt,
        Instant joinedAt,
        Instant leftAt,
        Instant createdAt,
        Instant updatedAt
) {
    public ChatRoomMemberSnapshot {
        Objects.requireNonNull(tenantId);
        Objects.requireNonNull(chatRoomId);
        Objects.requireNonNull(userId);
        Objects.requireNonNull(joinedAt);
        Objects.requireNonNull(createdAt);
        Objects.requireNonNull(updatedAt);
    }
}
