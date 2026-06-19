package com.polarishb.pabal.tenant.application.command.output;

import java.time.Instant;
import java.util.UUID;

public record CreateTenantResult(
        UUID tenantId,
        String name,
        String status,
        Instant createdAt
) {
}
