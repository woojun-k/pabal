package com.polarishb.pabal.tenant.domain.model.snapshot;

import com.polarishb.pabal.tenant.domain.model.type.TenantRegistrationStatus;
import com.polarishb.pabal.tenant.domain.model.vo.TenantDomainName;
import com.polarishb.pabal.tenant.domain.model.vo.TenantName;
import com.polarishb.pabal.tenant.domain.model.vo.TenantVerificationToken;

import java.time.Instant;
import java.util.Objects;
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
    public TenantRegistrationSnapshot {
        Objects.requireNonNull(tenantName, "tenantName must not be null");
        Objects.requireNonNull(domainName, "domainName must not be null");
        Objects.requireNonNull(verificationToken, "verificationToken must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }
}
