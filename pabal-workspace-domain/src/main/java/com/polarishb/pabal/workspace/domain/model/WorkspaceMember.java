package com.polarishb.pabal.workspace.domain.model;

import com.polarishb.pabal.workspace.domain.exception.WorkspaceMemberLeaveNotAllowedException;
import com.polarishb.pabal.workspace.domain.exception.WorkspaceMemberRoleChangeNotAllowedException;
import com.polarishb.pabal.workspace.domain.model.snapshot.WorkspaceMemberSnapshot;
import com.polarishb.pabal.workspace.domain.model.type.WorkspaceMemberStatus;
import com.polarishb.pabal.workspace.domain.model.type.WorkspaceRole;
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
public class WorkspaceMember {

    @EqualsAndHashCode.Include
    private final UUID id;
    private final UUID tenantId;
    private final UUID workspaceId;
    private final UUID userId;
    private final WorkspaceRole role;
    private final WorkspaceMemberStatus status;
    private final Instant joinedAt;
    private final Instant leftAt;
    private final Instant createdAt;
    private final Instant updatedAt;

    public static WorkspaceMember joinOwner(UUID tenantId, UUID workspaceId, UUID userId, Instant joinedAt) {
        return join(tenantId, workspaceId, userId, WorkspaceRole.OWNER, joinedAt);
    }

    public static WorkspaceMember join(
            UUID tenantId,
            UUID workspaceId,
            UUID userId,
            WorkspaceRole role,
            Instant joinedAt
    ) {
        return new WorkspaceMember(
                null,
                Objects.requireNonNull(tenantId, "tenantId must not be null"),
                Objects.requireNonNull(workspaceId, "workspaceId must not be null"),
                Objects.requireNonNull(userId, "userId must not be null"),
                Objects.requireNonNull(role, "role must not be null"),
                WorkspaceMemberStatus.ACTIVE,
                Objects.requireNonNull(joinedAt, "joinedAt must not be null"),
                null,
                joinedAt,
                joinedAt
        );
    }

    public static WorkspaceMember reconstitute(WorkspaceMemberSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        return new WorkspaceMember(
                snapshot.id(),
                snapshot.tenantId(),
                snapshot.workspaceId(),
                snapshot.userId(),
                snapshot.role(),
                snapshot.status(),
                snapshot.joinedAt(),
                snapshot.leftAt(),
                snapshot.createdAt(),
                snapshot.updatedAt()
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

    public boolean isActive() {
        return status == WorkspaceMemberStatus.ACTIVE;
    }

    public WorkspaceMember changeRole(WorkspaceRole newRole, Instant changedAt, boolean hasAnotherActiveOwner) {
        WorkspaceRole requestedRole = Objects.requireNonNull(newRole, "newRole must not be null");
        Instant transitionAt = Objects.requireNonNull(changedAt, "changedAt must not be null");

        if (status != WorkspaceMemberStatus.ACTIVE) {
            throw new WorkspaceMemberRoleChangeNotAllowedException(
                    id,
                    role,
                    requestedRole,
                    status,
                    "workspace member must be ACTIVE to change role"
            );
        }

        if (role == requestedRole) {
            return this;
        }

        if (role == WorkspaceRole.OWNER && !hasAnotherActiveOwner) {
            throw new WorkspaceMemberRoleChangeNotAllowedException(
                    id,
                    role,
                    requestedRole,
                    status,
                    "last active OWNER cannot be demoted"
            );
        }

        return new WorkspaceMember(
                id,
                tenantId,
                workspaceId,
                userId,
                requestedRole,
                status,
                joinedAt,
                leftAt,
                createdAt,
                transitionAt
        );
    }

    public WorkspaceMember leave(Instant leftAt, boolean hasAnotherActiveOwner) {
        Instant transitionAt = Objects.requireNonNull(leftAt, "leftAt must not be null");

        if (status != WorkspaceMemberStatus.ACTIVE) {
            throw new WorkspaceMemberLeaveNotAllowedException(
                    id,
                    role,
                    status,
                    "workspace member must be ACTIVE to leave"
            );
        }

        if (role == WorkspaceRole.OWNER && !hasAnotherActiveOwner) {
            throw new WorkspaceMemberLeaveNotAllowedException(
                    id,
                    role,
                    status,
                    "last active OWNER cannot leave"
            );
        }

        return new WorkspaceMember(
                id,
                tenantId,
                workspaceId,
                userId,
                role,
                WorkspaceMemberStatus.LEFT,
                joinedAt,
                transitionAt,
                createdAt,
                transitionAt
        );
    }
}
