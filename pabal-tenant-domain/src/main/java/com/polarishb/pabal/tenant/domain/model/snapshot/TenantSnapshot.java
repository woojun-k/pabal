package com.polarishb.pabal.tenant.domain.model.snapshot;

import com.polarishb.pabal.tenant.domain.model.type.TenantStatus;
import com.polarishb.pabal.tenant.domain.model.vo.TenantName;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record TenantSnapshot(
        UUID id,
        TenantName name,
        TenantStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public TenantSnapshot {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }
}
