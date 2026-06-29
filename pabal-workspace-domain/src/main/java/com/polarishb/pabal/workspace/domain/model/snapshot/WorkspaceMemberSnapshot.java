package com.polarishb.pabal.workspace.domain.model.snapshot;

import com.polarishb.pabal.workspace.domain.model.type.WorkspaceMemberStatus;
import com.polarishb.pabal.workspace.domain.model.type.WorkspaceRole;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record WorkspaceMemberSnapshot(
        UUID id,
        UUID tenantId,
        UUID workspaceId,
        UUID userId,
        WorkspaceRole role,
        WorkspaceMemberStatus status,
        Instant joinedAt,
        Instant leftAt,
        Instant createdAt,
        Instant updatedAt
) {
    public WorkspaceMemberSnapshot {
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(role, "role must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(joinedAt, "joinedAt must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        if (status == WorkspaceMemberStatus.LEFT && leftAt == null) {
            throw new IllegalArgumentException("leftAt is required when workspace member status is LEFT");
        }
        if (status == WorkspaceMemberStatus.ACTIVE && leftAt != null) {
            throw new IllegalArgumentException("leftAt must be null when workspace member status is ACTIVE");
        }
    }
}
