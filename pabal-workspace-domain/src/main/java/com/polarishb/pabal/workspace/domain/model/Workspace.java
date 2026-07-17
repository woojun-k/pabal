package com.polarishb.pabal.workspace.domain.model;

import com.polarishb.pabal.workspace.domain.model.snapshot.WorkspaceSnapshot;
import com.polarishb.pabal.workspace.domain.model.type.WorkspaceStatus;
import com.polarishb.pabal.workspace.domain.model.vo.WorkspaceName;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Workspace {

    @EqualsAndHashCode.Include
    private final UUID id;
    private final UUID tenantId;
    private final WorkspaceName name;
    private final WorkspaceStatus status;
    private final UUID createdBy;
    private final Instant createdAt;
    private final Instant updatedAt;

    public static Workspace create(UUID tenantId, String name, UUID createdBy, Instant createdAt) {
        return new Workspace(
                null,
                Objects.requireNonNull(tenantId, "tenantId must not be null"),
                new WorkspaceName(name),
                WorkspaceStatus.ACTIVE,
                Objects.requireNonNull(createdBy, "createdBy must not be null"),
                Objects.requireNonNull(createdAt, "createdAt must not be null"),
                createdAt
        );
    }

    public static Workspace reconstitute(WorkspaceSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        return new Workspace(
                snapshot.id(),
                snapshot.tenantId(),
                snapshot.name(),
                snapshot.status(),
                snapshot.createdBy(),
                snapshot.createdAt(),
                snapshot.updatedAt()
        );
    }

    public WorkspaceSnapshot snapshot() {
        return new WorkspaceSnapshot(id, tenantId, name, status, createdBy, createdAt, updatedAt);
    }

    public boolean isActive() {
        return status == WorkspaceStatus.ACTIVE;
    }
}
