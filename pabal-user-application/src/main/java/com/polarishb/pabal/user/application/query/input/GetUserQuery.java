package com.polarishb.pabal.user.application.query.input;

import com.polarishb.pabal.common.cqrs.Query;

import java.util.UUID;

public record GetUserQuery(
        UUID tenantId,
        UUID userId
) implements Query {
}
