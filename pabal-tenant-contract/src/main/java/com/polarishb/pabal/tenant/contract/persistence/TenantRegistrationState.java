package com.polarishb.pabal.tenant.contract.persistence;

import com.polarishb.pabal.tenant.domain.model.snapshot.TenantRegistrationSnapshot;
import com.polarishb.pabal.tenant.domain.model.type.TenantRegistrationStatus;
import com.polarishb.pabal.tenant.domain.model.vo.TenantDomainName;
import com.polarishb.pabal.tenant.domain.model.vo.TenantName;
import com.polarishb.pabal.tenant.domain.model.vo.TenantVerificationToken;

import java.time.Instant;
import java.util.UUID;

public record TenantRegistrationState(
        TenantRegistrationSnapshot snapshot,
        Long version
) {
    public TenantRegistrationState(
            UUID id,
            String tenantName,
            String domainName,
            String verificationToken,
            TenantRegistrationStatus status,
            Instant expiresAt,
            Instant verifiedAt,
            Instant activatedAt,
            UUID tenantId,
            Instant createdAt,
            Instant updatedAt,
            Long version
    ) {
        this(new TenantRegistrationSnapshot(
                id,
                new TenantName(tenantName),
                new TenantDomainName(domainName),
                new TenantVerificationToken(verificationToken),
                status,
                expiresAt,
                verifiedAt,
                activatedAt,
                tenantId,
                createdAt,
                updatedAt
        ), version);
    }

    public UUID id() {
        return snapshot.id();
    }

    public String tenantName() {
        return snapshot.tenantName().value();
    }

    public String domainName() {
        return snapshot.domainName().value();
    }

    public String verificationToken() {
        return snapshot.verificationToken().value();
    }

    public TenantRegistrationStatus status() {
        return snapshot.status();
    }

    public Instant expiresAt() {
        return snapshot.expiresAt();
    }

    public Instant verifiedAt() {
        return snapshot.verifiedAt();
    }

    public Instant activatedAt() {
        return snapshot.activatedAt();
    }

    public UUID tenantId() {
        return snapshot.tenantId();
    }

    public Instant createdAt() {
        return snapshot.createdAt();
    }

    public Instant updatedAt() {
        return snapshot.updatedAt();
    }
}
