package com.polarishb.pabal.tenant.contract.persistence;

import com.polarishb.pabal.tenant.domain.model.TenantRegistration;

import java.util.Objects;

public record PersistedTenantRegistration(
        TenantRegistration registration,
        TenantRegistrationState state
) {
    public PersistedTenantRegistration {
        Objects.requireNonNull(registration);
        Objects.requireNonNull(state);
    }
}
