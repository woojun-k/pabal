package com.polarishb.pabal.user.api.query.http.response;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID userId,
        UUID tenantId,
        String name,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
