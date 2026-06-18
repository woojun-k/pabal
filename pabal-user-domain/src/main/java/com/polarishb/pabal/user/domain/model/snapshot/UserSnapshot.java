package com.polarishb.pabal.user.domain.model.snapshot;

import com.polarishb.pabal.user.domain.model.type.UserStatus;
import com.polarishb.pabal.user.domain.model.vo.UserName;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record UserSnapshot(
        UUID id,
        UUID tenantId,
        UserName name,
        UserStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public UserSnapshot {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }
}
