package com.polarishb.pabal.integration.contract;

import java.util.UUID;

public interface TenantContract {
    boolean existsActiveTenant(UUID tenantId);
}
