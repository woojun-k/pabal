package com.polarishb.pabal.tenant.api.command.http.response;

import java.time.Instant;
import java.util.UUID;

public record TenantRegistrationResponse(
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
