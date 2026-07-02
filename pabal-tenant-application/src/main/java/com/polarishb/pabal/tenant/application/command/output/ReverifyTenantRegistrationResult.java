package com.polarishb.pabal.tenant.application.command.output;

import java.time.Instant;
import java.util.UUID;

public record ReverifyTenantRegistrationResult(
        UUID registrationId,
        String tenantName,
        String domainName,
        String status,
        Instant verifiedAt,
        Instant activationExpiresAt
) {
}
