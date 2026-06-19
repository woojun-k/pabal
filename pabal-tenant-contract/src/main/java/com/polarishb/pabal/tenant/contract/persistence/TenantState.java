package com.polarishb.pabal.tenant.contract.persistence;

import com.polarishb.pabal.tenant.domain.model.snapshot.TenantSnapshot;
import com.polarishb.pabal.tenant.domain.model.type.TenantStatus;
import com.polarishb.pabal.tenant.domain.model.vo.TenantName;

import java.time.Instant;
import java.util.UUID;

public record TenantState(
        TenantSnapshot snapshot,
        Long version
) {
    public TenantState(
            UUID id,
            String name,
            TenantStatus status,
            Instant createdAt,
            Instant updatedAt,
            Long version
    ) {
        this(new TenantSnapshot(id, new TenantName(name), status, createdAt, updatedAt), version);
    }

    public UUID id() {
        return snapshot.id();
    }

    public String name() {
        return snapshot.name().value();
    }

    public TenantStatus status() {
        return snapshot.status();
    }

    public Instant createdAt() {
        return snapshot.createdAt();
    }

    public Instant updatedAt() {
        return snapshot.updatedAt();
    }
}
