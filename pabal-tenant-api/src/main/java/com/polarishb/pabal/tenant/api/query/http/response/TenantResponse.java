package com.polarishb.pabal.tenant.api.query.http.response;

import java.time.Instant;
import java.util.UUID;

public record TenantResponse(
        UUID tenantId,
        String name,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
