package com.polarishb.pabal.tenant.api.command.http.response;

import java.time.Instant;
import java.util.UUID;

public record ReverifyTenantRegistrationResponse(
        UUID registrationId,
        String tenantName,
        String domainName,
        String status,
        Instant verifiedAt,
        Instant activationExpiresAt
) {
}
