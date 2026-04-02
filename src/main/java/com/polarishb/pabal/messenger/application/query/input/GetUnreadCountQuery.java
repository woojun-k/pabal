package com.polarishb.pabal.messenger.application.query.input;

import com.polarishb.pabal.common.cqrs.Query;

import java.util.UUID;

public record GetUnreadCountQuery(
        UUID tenantId,
        UUID chatRoomId,
        UUID userId
) implements Query {
}
