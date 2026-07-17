package com.polarishb.pabal.integration.contract;

import com.polarishb.pabal.integration.contract.dto.UserInfo;

import java.util.Set;
import java.util.UUID;

public interface UserContract {
    boolean existsUserInTenant(UUID userId, UUID tenantId);
    Set<UUID> findActiveUserIdsInTenant(UUID tenantId, Set<UUID> userIds);
    UserInfo getUserInfo(UUID userId);
}
