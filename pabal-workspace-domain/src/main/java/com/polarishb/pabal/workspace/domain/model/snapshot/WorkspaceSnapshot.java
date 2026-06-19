package com.polarishb.pabal.workspace.domain.model.snapshot;

import com.polarishb.pabal.workspace.domain.model.type.WorkspaceStatus;
import com.polarishb.pabal.workspace.domain.model.vo.WorkspaceName;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record WorkspaceSnapshot(
        UUID id,
        UUID tenantId,
        WorkspaceName name,
        WorkspaceStatus status,
        UUID createdBy,
        Instant createdAt,
        Instant updatedAt
) {
    public WorkspaceSnapshot {
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(createdBy, "createdBy must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }
}
