package com.polarishb.pabal.messenger.contract.realtime;

import java.time.Instant;
import java.util.UUID;

public record MemberJoinedRealtimePayload(
    UUID userId,
    UUID chatRoomId,
    Instant joinedAt
) {}
