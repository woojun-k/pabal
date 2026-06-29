package com.polarishb.pabal.workspace.contract.persistence;

import com.polarishb.pabal.workspace.domain.model.snapshot.WorkspaceMemberSnapshot;
import com.polarishb.pabal.workspace.domain.model.type.WorkspaceMemberStatus;
import com.polarishb.pabal.workspace.domain.model.type.WorkspaceRole;

import java.time.Instant;
import java.util.UUID;

public record WorkspaceMemberState(
        WorkspaceMemberSnapshot snapshot,
        Long version
) {
    public WorkspaceMemberState(
            UUID id,
            UUID tenantId,
            UUID workspaceId,
            UUID userId,
            WorkspaceRole role,
            WorkspaceMemberStatus status,
            Instant joinedAt,
            Instant leftAt,
            Instant createdAt,
            Instant updatedAt,
            Long version
    ) {
        this(new WorkspaceMemberSnapshot(
                id,
                tenantId,
                workspaceId,
                userId,
                role,
                status,
                joinedAt,
                leftAt,
                createdAt,
                updatedAt
        ), version);
    }

    public UUID id() {
        return snapshot.id();
    }

    public UUID tenantId() {
        return snapshot.tenantId();
    }

    public UUID workspaceId() {
        return snapshot.workspaceId();
    }

    public UUID userId() {
        return snapshot.userId();
    }

    public WorkspaceRole role() {
        return snapshot.role();
    }

    public WorkspaceMemberStatus status() {
        return snapshot.status();
    }

    public Instant joinedAt() {
        return snapshot.joinedAt();
    }

    public Instant leftAt() {
        return snapshot.leftAt();
    }

    public Instant createdAt() {
        return snapshot.createdAt();
    }

    public Instant updatedAt() {
        return snapshot.updatedAt();
    }
}
