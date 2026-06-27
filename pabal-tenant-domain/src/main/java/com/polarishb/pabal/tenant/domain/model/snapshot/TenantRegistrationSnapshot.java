package com.polarishb.pabal.tenant.domain.model.snapshot;

import com.polarishb.pabal.tenant.domain.model.type.TenantRegistrationStatus;
import com.polarishb.pabal.tenant.domain.model.vo.TenantDomainName;
import com.polarishb.pabal.tenant.domain.model.vo.TenantName;
import com.polarishb.pabal.tenant.domain.model.vo.TenantVerificationToken;

import java.time.Instant;
import java.util.UUID;

public record TenantRegistrationSnapshot(
        UUID id,
        TenantName tenantName,
        TenantDomainName domainName,
        TenantVerificationToken verificationToken,
        TenantRegistrationStatus status,
        Instant expiresAt,
        Instant verifiedAt,
        Instant activatedAt,
        UUID tenantId,
        Instant createdAt,
        Instant updatedAt
) {
}
