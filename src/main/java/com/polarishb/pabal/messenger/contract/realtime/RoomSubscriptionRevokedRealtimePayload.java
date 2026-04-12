package com.polarishb.pabal.messenger.contract.realtime;

import java.time.Instant;
import java.util.UUID;

public record RoomSubscriptionRevokedRealtimePayload(
    UUID tenantId,
    UUID chatRoomId,
    Instant revokedAt
) {
}
