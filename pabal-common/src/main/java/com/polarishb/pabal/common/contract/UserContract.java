package com.polarishb.pabal.common.contract;

import com.polarishb.pabal.common.contract.dto.UserInfo;

import java.util.Set;
import java.util.UUID;

public interface UserContract {
    boolean existsUserInTenant(UUID userId, UUID tenantId);
    Set<UUID> findActiveUserIdsInTenant(UUID tenantId, Set<UUID> userIds);
    UserInfo getUserInfo(UUID userId);
}
