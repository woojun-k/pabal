package com.polarishb.pabal.common.contract;

import com.polarishb.pabal.common.contract.dto.UserInfo;

import java.util.UUID;

public interface UserContract {
    boolean existsUserInTenant(UUID userId, UUID tenantId);
    UserInfo getUserInfo(UUID userId);
}
