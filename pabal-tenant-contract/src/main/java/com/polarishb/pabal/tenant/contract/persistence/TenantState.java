package com.polarishb.pabal.tenant.contract.persistence;

import com.polarishb.pabal.tenant.domain.model.snapshot.TenantSnapshot;
import com.polarishb.pabal.tenant.domain.model.type.TenantStatus;
import com.polarishb.pabal.tenant.domain.model.vo.TenantName;

import java.time.Instant;
import java.util.UUID;

public record TenantState(
        UUID id,
        String name,
        TenantStatus status,
        Instant createdAt,
        Instant updatedAt,
        Long version
) {
    public TenantState(
            TenantSnapshot snapshot,
            Long version
    ) {
        this(
                snapshot.id(),
                snapshot.name().value(),
                snapshot.status(),
                snapshot.createdAt(),
                snapshot.updatedAt(),
                version
        );
    }

    public TenantSnapshot snapshot() {
        return new TenantSnapshot(id, new TenantName(name), status, createdAt, updatedAt);
    }
}
