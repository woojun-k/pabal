package com.polarishb.pabal.tenant.application.command.output;

import java.time.Instant;
import java.util.UUID;

public record VerifyTenantDomainResult(
        UUID registrationId,
        UUID tenantId,
        String tenantName,
        String domainName,
        String status,
        Instant verifiedAt,
        Instant activatedAt
) {
}
