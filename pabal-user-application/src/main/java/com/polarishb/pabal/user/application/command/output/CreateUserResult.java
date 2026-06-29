package com.polarishb.pabal.user.application.command.output;

import java.time.Instant;
import java.util.UUID;

public record CreateUserResult(
        UUID userId,
        UUID tenantId,
        String name,
        String status,
        Instant createdAt
) {
}
