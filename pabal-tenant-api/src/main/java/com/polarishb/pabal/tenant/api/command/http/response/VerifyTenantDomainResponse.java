package com.polarishb.pabal.tenant.api.command.http.response;

import java.time.Instant;
import java.util.UUID;

public record VerifyTenantDomainResponse(
        UUID registrationId,
        UUID tenantId,
        String tenantName,
        String domainName,
        String status,
        Instant verifiedAt,
        Instant activatedAt
) {
}
