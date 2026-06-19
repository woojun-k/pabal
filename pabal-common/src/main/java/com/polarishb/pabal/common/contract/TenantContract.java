package com.polarishb.pabal.common.contract;

import java.util.UUID;

public interface TenantContract {
    boolean existsActiveTenant(UUID tenantId);
}
