package com.polarishb.pabal.messenger.contract.realtime;

import java.time.Instant;
import java.util.UUID;

public record MessageReadRealtimePayload(
    UUID userId,
    UUID chatRoomId,
    UUID lastReadMessageId,
    Instant readAt
) {}
