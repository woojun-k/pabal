package com.polarishb.pabal.workspace.contract.persistence;

import com.polarishb.pabal.workspace.domain.model.snapshot.WorkspaceSnapshot;
import com.polarishb.pabal.workspace.domain.model.type.WorkspaceStatus;
import com.polarishb.pabal.workspace.domain.model.vo.WorkspaceName;

import java.time.Instant;
import java.util.UUID;

public record WorkspaceState(
        UUID id,
        UUID tenantId,
        String name,
        WorkspaceStatus status,
        UUID createdBy,
        Instant createdAt,
        Instant updatedAt,
        Long version
) {
    public WorkspaceState(
            WorkspaceSnapshot snapshot,
            Long version
    ) {
        this(
                snapshot.id(),
                snapshot.tenantId(),
                snapshot.name().value(),
                snapshot.status(),
                snapshot.createdBy(),
                snapshot.createdAt(),
                snapshot.updatedAt(),
                version
        );
    }

    public WorkspaceSnapshot snapshot() {
        return new WorkspaceSnapshot(
                id,
                tenantId,
                new WorkspaceName(name),
                status,
                createdBy,
                createdAt,
                updatedAt
        );
    }
}
