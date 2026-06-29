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

    /**
     * 불변 도메인 전이 결과(next)를 조회 당시 persistence 기준점(state)에 다시 묶는다.
     */
    public PersistedTenantRegistration withRegistration(TenantRegistration next) {
        Objects.requireNonNull(next);
        if (!Objects.equals(next.getId(), state.id())) {
            throw new IllegalArgumentException("registration id must match persisted state id");
        }
        if (!Objects.equals(next.getTenantName().value(), state.tenantName())) {
            throw new IllegalArgumentException("registration tenantName must match persisted state tenantName");
        }
        if (!Objects.equals(next.getDomainName().value(), state.domainName())) {
            throw new IllegalArgumentException("registration domainName must match persisted state domainName");
        }
        return new PersistedTenantRegistration(next, state);
    }
}
