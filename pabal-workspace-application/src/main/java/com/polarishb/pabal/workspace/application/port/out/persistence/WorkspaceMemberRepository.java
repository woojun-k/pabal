package com.polarishb.pabal.workspace.application.port.out.persistence;

import com.polarishb.pabal.workspace.contract.persistence.PersistedWorkspaceMember;

import java.util.Set;
import java.util.UUID;

public interface WorkspaceMemberRepository {
    PersistedWorkspaceMember append(PersistedWorkspaceMember member);
    boolean existsActiveMember(UUID tenantId, UUID workspaceId, UUID userId);
    Set<UUID> findActiveUserIds(UUID tenantId, UUID workspaceId, Set<UUID> userIds);
}
