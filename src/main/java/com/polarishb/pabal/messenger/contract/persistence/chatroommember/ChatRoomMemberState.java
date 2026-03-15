package com.polarishb.pabal.messenger.contract.persistence.chatroommember;

import java.time.Instant;
import java.util.UUID;

public record ChatRoomMemberState(
    UUID id,
    UUID tenantId,
    UUID chatRoomId,
    UUID userId,
    UUID lastReadMessageId,
    Instant lastReadAt,
    Instant joinedAt,
    Instant leftAt,
    Long version
) {}
