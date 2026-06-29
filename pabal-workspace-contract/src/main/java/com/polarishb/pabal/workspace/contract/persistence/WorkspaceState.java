package com.polarishb.pabal.workspace.contract.persistence;

import com.polarishb.pabal.workspace.domain.model.snapshot.WorkspaceSnapshot;
import com.polarishb.pabal.workspace.domain.model.type.WorkspaceStatus;
import com.polarishb.pabal.workspace.domain.model.vo.WorkspaceName;

import java.time.Instant;
import java.util.UUID;

public record WorkspaceState(
        WorkspaceSnapshot snapshot,
        Long version
) {
    public WorkspaceState(
            UUID id,
            UUID tenantId,
            String name,
            WorkspaceStatus status,
            UUID createdBy,
            Instant createdAt,
            Instant updatedAt,
            Long version
    ) {
        this(new WorkspaceSnapshot(
                id,
                tenantId,
                new WorkspaceName(name),
                status,
                createdBy,
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

    public String name() {
        return snapshot.name().value();
    }

    public WorkspaceStatus status() {
        return snapshot.status();
    }

    public UUID createdBy() {
        return snapshot.createdBy();
    }

    public Instant createdAt() {
        return snapshot.createdAt();
    }

    public Instant updatedAt() {
        return snapshot.updatedAt();
    }
}
