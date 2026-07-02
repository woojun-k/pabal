package com.polarishb.pabal.tenant.contract.persistence;

import com.polarishb.pabal.tenant.domain.model.snapshot.TenantRegistrationSnapshot;
import com.polarishb.pabal.tenant.domain.model.type.TenantRegistrationStatus;
import com.polarishb.pabal.tenant.domain.model.vo.TenantDomainName;
import com.polarishb.pabal.tenant.domain.model.vo.TenantName;
import com.polarishb.pabal.tenant.domain.model.vo.TenantVerificationToken;

import java.time.Instant;
import java.util.UUID;

public record TenantRegistrationState(
        UUID id,
        String tenantName,
        String domainName,
        String verificationToken,
        TenantRegistrationStatus status,
        Instant verificationExpiresAt,
        Instant activationExpiresAt,
        Instant verifiedAt,
        Instant activatedAt,
        UUID tenantId,
        Instant createdAt,
        Instant updatedAt,
        Long version
) {
    public TenantRegistrationState(
            TenantRegistrationSnapshot snapshot,
            Long version
    ) {
        this(
                snapshot.id(),
                snapshot.tenantName().value(),
                snapshot.domainName().value(),
                snapshot.verificationToken().value(),
                snapshot.status(),
                snapshot.verificationExpiresAt(),
                snapshot.activationExpiresAt(),
                snapshot.verifiedAt(),
                snapshot.activatedAt(),
                snapshot.tenantId(),
                snapshot.createdAt(),
                snapshot.updatedAt(),
                version
        );
    }

    public TenantRegistrationSnapshot snapshot() {
        return new TenantRegistrationSnapshot(
                id,
                new TenantName(tenantName),
                new TenantDomainName(domainName),
                new TenantVerificationToken(verificationToken),
                status,
                verificationExpiresAt,
                activationExpiresAt,
                verifiedAt,
                activatedAt,
                tenantId,
                createdAt,
                updatedAt
        );
    }
}
