package com.polarishb.pabal.messenger.domain.repository;

import java.time.Instant;
import java.util.UUID;

public interface ChatRoomSequenceRepository {

    long allocateNextMessageSequence(UUID tenantId, UUID chatRoomId);

    void updateLastMessageSnapshot(
            UUID tenantId,
            UUID chatRoomId,
            UUID messageId,
            long messageSequence,
            Instant messageAt
    );
}
