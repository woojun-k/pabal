package com.polarishb.pabal.workspace.contract.persistence;

import com.polarishb.pabal.workspace.domain.model.snapshot.WorkspaceMemberSnapshot;
import com.polarishb.pabal.workspace.domain.model.type.WorkspaceMemberStatus;
import com.polarishb.pabal.workspace.domain.model.type.WorkspaceRole;

import java.time.Instant;
import java.util.UUID;

public record WorkspaceMemberState(
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
    public WorkspaceMemberState(
            WorkspaceMemberSnapshot snapshot,
            Long version
    ) {
        this(
                snapshot.id(),
                snapshot.tenantId(),
                snapshot.workspaceId(),
                snapshot.userId(),
                snapshot.role(),
                snapshot.status(),
                snapshot.joinedAt(),
                snapshot.leftAt(),
                snapshot.createdAt(),
                snapshot.updatedAt(),
                version
        );
    }

    public WorkspaceMemberSnapshot snapshot() {
        return new WorkspaceMemberSnapshot(
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
        );
    }
}
