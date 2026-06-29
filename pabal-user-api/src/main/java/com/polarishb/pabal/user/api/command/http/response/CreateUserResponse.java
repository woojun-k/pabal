package com.polarishb.pabal.user.api.command.http.response;

import java.time.Instant;
import java.util.UUID;

public record CreateUserResponse(
        UUID userId,
        UUID tenantId,
        String name,
        String status,
        Instant createdAt
) {
}
