package com.polarishb.pabal.common.contract;

import java.util.Set;
import java.util.UUID;

public interface WorkspaceContract {
    boolean existsActiveMember(UUID tenantId, UUID workspaceId, UUID userId);
    Set<UUID> findActiveMemberIds(UUID tenantId, UUID workspaceId, Set<UUID> userIds);
}
