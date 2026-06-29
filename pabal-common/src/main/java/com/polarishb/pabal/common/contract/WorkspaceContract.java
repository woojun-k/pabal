package com.polarishb.pabal.common.contract;

import com.polarishb.pabal.common.contract.dto.WorkspaceMemberRole;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface WorkspaceContract {
    boolean existsActiveMember(UUID tenantId, UUID workspaceId, UUID userId);
    Set<UUID> findActiveMemberIds(UUID tenantId, UUID workspaceId, Set<UUID> userIds);
    Optional<WorkspaceMemberRole> findActiveMemberRole(UUID tenantId, UUID workspaceId, UUID userId);
}
