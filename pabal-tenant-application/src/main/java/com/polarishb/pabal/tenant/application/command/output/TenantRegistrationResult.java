package com.polarishb.pabal.tenant.application.command.output;

import java.time.Instant;
import java.util.UUID;

public record TenantRegistrationResult(
        UUID registrationId,
        String tenantName,
        String domainName,
        String status,
        String verificationDnsName,
        String verificationTxtValue,
        Instant expiresAt,
        Instant createdAt
) {
}
