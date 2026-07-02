package com.polarishb.pabal.tenant.api.query.http.response;

import java.time.Instant;
import java.util.UUID;

public record TenantRegistrationDetailResponse(
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
