package com.polarishb.pabal.tenant.application.query.output;

import java.time.Instant;
import java.util.UUID;

public record TenantRegistrationDto(
        UUID registrationId,
        UUID tenantId,
        String tenantName,
        String domainName,
        String status,
        String verificationDnsName,
        String verificationTxtValue,
        Instant verificationExpiresAt,
        Instant activationExpiresAt,
        Instant verifiedAt,
        Instant activatedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
