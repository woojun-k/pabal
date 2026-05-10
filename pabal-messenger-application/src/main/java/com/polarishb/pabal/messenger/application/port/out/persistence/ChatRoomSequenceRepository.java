package com.polarishb.pabal.messenger.application.port.out.persistence;

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
