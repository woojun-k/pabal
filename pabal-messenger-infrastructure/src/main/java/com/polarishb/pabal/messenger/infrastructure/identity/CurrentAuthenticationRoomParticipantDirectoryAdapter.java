package com.polarishb.pabal.messenger.infrastructure.identity;

import com.polarishb.pabal.common.contract.UserContract;
import com.polarishb.pabal.messenger.application.port.out.identity.RoomParticipantDirectoryPort;
import com.polarishb.pabal.security.context.CurrentAuthenticationProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CurrentAuthenticationRoomParticipantDirectoryAdapter implements RoomParticipantDirectoryPort {

    private final CurrentAuthenticationProvider currentAuthenticationProvider;
    private final ObjectProvider<UserContract> userContractProvider;

    @Override
    public Set<UUID> findTenantMemberIds(UUID tenantId, Set<UUID> userIds) {
        Set<UUID> requestedUserIds = normalize(userIds);
        Set<UUID> memberIds = currentPrincipalMemberIds(tenantId, requestedUserIds);

        UserContract userContract = userContractProvider.getIfAvailable();
        if (userContract != null) {
            requestedUserIds.stream()
                    .filter(userId -> userContract.existsUserInTenant(userId, tenantId))
                    .forEach(memberIds::add);
        }

        return Set.copyOf(memberIds);
    }

    @Override
    public Set<UUID> findWorkspaceMemberIds(UUID tenantId, UUID workspaceId, Set<UUID> userIds) {
        Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        return Set.copyOf(currentPrincipalMemberIds(tenantId, normalize(userIds)));
    }

    private Set<UUID> currentPrincipalMemberIds(UUID tenantId, Set<UUID> requestedUserIds) {
        Set<UUID> memberIds = new LinkedHashSet<>();
        currentAuthenticationProvider.currentAuthentication()
                .filter(authentication -> tenantId.equals(authentication.principal().tenantId()))
                .map(authentication -> authentication.principal().userId())
                .filter(requestedUserIds::contains)
                .ifPresent(memberIds::add);
        return memberIds;
    }

    private Set<UUID> normalize(Set<UUID> userIds) {
        Objects.requireNonNull(userIds, "userIds must not be null");
        Set<UUID> normalized = new LinkedHashSet<>();
        userIds.stream()
                .map(userId -> Objects.requireNonNull(userId, "userId must not be null"))
                .forEach(normalized::add);
        return normalized;
    }
}
