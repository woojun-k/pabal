package com.polarishb.pabal.tenant.contract.persistence;

import com.polarishb.pabal.tenant.domain.model.TenantRegistration;

public final class TenantRegistrationPersistenceMapper {

    private TenantRegistrationPersistenceMapper() {
    }

    public static TenantRegistration toDomain(TenantRegistrationState state) {
        return TenantRegistration.reconstitute(state.snapshot());
    }

    public static TenantRegistrationState toState(TenantRegistration registration, Long version) {
        return new TenantRegistrationState(registration.snapshot(), version);
    }

    public static PersistedTenantRegistration toPersisted(TenantRegistrationState state) {
        return new PersistedTenantRegistration(toDomain(state), state);
    }
}
