package com.polarishb.pabal.tenant.contract.persistence;

import com.polarishb.pabal.tenant.domain.model.Tenant;

import java.util.Objects;

public record PersistedTenant(
        Tenant tenant,
        TenantState state
) {
    public PersistedTenant {
        Objects.requireNonNull(tenant);
        Objects.requireNonNull(state);
    }
}
