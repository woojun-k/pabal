package com.polarishb.pabal.messenger.infrastructure.identity;

import com.polarishb.pabal.common.contract.UserContract;
import com.polarishb.pabal.common.contract.WorkspaceContract;
import com.polarishb.pabal.messenger.application.port.out.identity.RoomParticipantDirectoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ContractRoomParticipantDirectoryAdapter implements RoomParticipantDirectoryPort {

    private final UserContract userContract;
    private final WorkspaceContract workspaceContract;

    @Override
    public boolean existsActiveTenantMember(UUID tenantId, UUID userId) {
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
        return userContract.existsUserInTenant(userId, tenantId);
    }

    @Override
    public Set<UUID> findTenantMemberIds(UUID tenantId, Set<UUID> userIds) {
        Set<UUID> requestedUserIds = normalize(userIds);
        if (requestedUserIds.isEmpty()) {
            return Set.of();
        }

        return userContract.findActiveUserIdsInTenant(tenantId, requestedUserIds);
    }

    @Override
    public Set<UUID> findWorkspaceMemberIds(UUID tenantId, UUID workspaceId, Set<UUID> userIds) {
        Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        Set<UUID> requestedUserIds = normalize(userIds);
        if (requestedUserIds.isEmpty()) {
            return Set.of();
        }

        return workspaceContract.findActiveMemberIds(tenantId, workspaceId, requestedUserIds);
    }

    private Set<UUID> normalize(Set<UUID> userIds) {
        Objects.requireNonNull(userIds, "userIds must not be null");
        userIds.forEach(userId -> Objects.requireNonNull(userId, "userId must not be null"));
        return userIds;
    }
}
