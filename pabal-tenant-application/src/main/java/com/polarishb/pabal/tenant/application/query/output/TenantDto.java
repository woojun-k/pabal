package com.polarishb.pabal.tenant.application.query.output;

import java.time.Instant;
import java.util.UUID;

public record TenantDto(
        UUID tenantId,
        String name,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
