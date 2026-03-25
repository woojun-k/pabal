package com.polarishb.pabal.messenger.contract.realtime;

import java.time.Instant;
import java.util.UUID;

public record MemberLeftRealtimePayload(
    UUID userId,
    UUID chatRoomId,
    Instant leftAt
) {}
