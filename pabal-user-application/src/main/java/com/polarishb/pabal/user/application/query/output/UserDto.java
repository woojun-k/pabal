package com.polarishb.pabal.user.application.query.output;

import java.time.Instant;
import java.util.UUID;

public record UserDto(
        UUID userId,
        UUID tenantId,
        String name,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
