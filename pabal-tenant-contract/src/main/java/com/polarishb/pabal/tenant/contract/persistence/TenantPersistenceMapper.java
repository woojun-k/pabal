package com.polarishb.pabal.tenant.contract.persistence;

import com.polarishb.pabal.tenant.domain.model.Tenant;

public final class TenantPersistenceMapper {

    private TenantPersistenceMapper() {
    }

    public static Tenant toDomain(TenantState state) {
        return Tenant.reconstitute(state.snapshot());
    }

    public static TenantState toState(Tenant tenant, Long version) {
        return new TenantState(tenant.snapshot(), version);
    }

    public static PersistedTenant toPersisted(TenantState state) {
        return new PersistedTenant(toDomain(state), state);
    }
}
