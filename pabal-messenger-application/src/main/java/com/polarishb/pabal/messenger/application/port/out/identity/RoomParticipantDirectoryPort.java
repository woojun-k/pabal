package com.polarishb.pabal.messenger.application.port.out.identity;

import java.util.Set;
import java.util.UUID;

public interface RoomParticipantDirectoryPort {

    Set<UUID> findTenantMemberIds(UUID tenantId, Set<UUID> userIds);

    Set<UUID> findWorkspaceMemberIds(UUID tenantId, UUID workspaceId, Set<UUID> userIds);
}
