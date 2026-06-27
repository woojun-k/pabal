package com.polarishb.pabal.tenant.api.dev.http.response;

import java.time.Instant;
import java.util.UUID;

public record TenantDevResponse(
        UUID tenantId,
        String name,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
