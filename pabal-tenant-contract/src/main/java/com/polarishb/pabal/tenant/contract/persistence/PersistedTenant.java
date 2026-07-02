package com.polarishb.pabal.tenant.contract.persistence;

import com.polarishb.pabal.tenant.domain.model.Tenant;

import java.util.Objects;

public record PersistedTenant(
        Tenant tenant,
        TenantState state
) {
    public PersistedTenant {
        Objects.requireNonNull(tenant, "tenant must not be null");
        Objects.requireNonNull(state, "state must not be null");
        if (!Objects.equals(tenant.getId(), state.id())) {
            throw new IllegalArgumentException("tenant id must match persisted state id");
        }
        if (!Objects.equals(tenant.getName().value(), state.name())) {
            throw new IllegalArgumentException("tenant name must match persisted state name");
        }
    }
}
