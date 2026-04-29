package com.polarishb.pabal.messenger.contract.realtime;

import java.time.Instant;
import java.util.UUID;

public record MessageDeletedRealtimePayload(
    UUID messageId,
    UUID chatRoomId,
    Instant deletedAt
) {}
