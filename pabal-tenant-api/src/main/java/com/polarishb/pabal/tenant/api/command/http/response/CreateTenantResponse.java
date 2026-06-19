package com.polarishb.pabal.tenant.api.command.http.response;

import java.time.Instant;
import java.util.UUID;

public record CreateTenantResponse(
        UUID tenantId,
        String name,
        String status,
        Instant createdAt
) {
}
