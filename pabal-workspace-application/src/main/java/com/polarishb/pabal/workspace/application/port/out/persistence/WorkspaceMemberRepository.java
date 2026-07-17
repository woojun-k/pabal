package com.polarishb.pabal.workspace.application.port.out.persistence;

import com.polarishb.pabal.workspace.contract.persistence.PersistedWorkspaceMember;
import com.polarishb.pabal.workspace.domain.model.type.WorkspaceRole;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface WorkspaceMemberRepository {
    PersistedWorkspaceMember append(PersistedWorkspaceMember member);
    PersistedWorkspaceMember update(PersistedWorkspaceMember member);
    boolean existsActiveMember(UUID tenantId, UUID workspaceId, UUID userId);
    Set<UUID> findActiveUserIds(UUID tenantId, UUID workspaceId, Set<UUID> userIds);
    Optional<WorkspaceRole> findActiveRole(UUID tenantId, UUID workspaceId, UUID userId);
}
