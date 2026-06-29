package com.polarishb.pabal.tenant.application.port.out.persistence;

import com.polarishb.pabal.tenant.contract.persistence.PersistedTenant;

import java.util.Optional;
import java.util.UUID;

public interface TenantRepository {
    PersistedTenant append(PersistedTenant tenant);
    Optional<PersistedTenant> findById(UUID tenantId);
    boolean existsActiveById(UUID tenantId);
}
