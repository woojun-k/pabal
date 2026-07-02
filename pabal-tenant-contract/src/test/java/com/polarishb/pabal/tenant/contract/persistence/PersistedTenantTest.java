package com.polarishb.pabal.tenant.contract.persistence;

import com.polarishb.pabal.tenant.domain.model.Tenant;
import com.polarishb.pabal.tenant.domain.model.snapshot.TenantSnapshot;
import com.polarishb.pabal.tenant.domain.model.type.TenantStatus;
import com.polarishb.pabal.tenant.domain.model.vo.TenantName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

class PersistedTenantTest {

    private static final Instant CREATED_AT = Instant.parse("2026-06-19T00:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-06-19T00:00:30Z");

    @Test
    void accepts_matching_tenant_and_state_pair() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = tenant(tenantId, "Acme");
        TenantState state = state(tenantId, "Acme");

        PersistedTenant persisted = new PersistedTenant(tenant, state);

        assertThat(persisted.tenant()).isSameAs(tenant);
        assertThat(persisted.state()).isSameAs(state);
    }

    @Test
    void rejects_null_tenant() {
        TenantState state = state(UUID.randomUUID(), "Acme");

        Throwable thrown = catchThrowable(() -> new PersistedTenant(null, state));

        assertThat(thrown).isNotNull();
    }

    @Test
    void rejects_null_state() {
        Tenant tenant = tenant(UUID.randomUUID(), "Acme");

        Throwable thrown = catchThrowable(() -> new PersistedTenant(tenant, null));

        assertThat(thrown).isNotNull();
    }

    @Test
    void rejects_mismatched_tenant_id() {
        Tenant tenant = tenant(UUID.randomUUID(), "Acme");
        TenantState state = state(UUID.randomUUID(), "Acme");

        assertThatThrownBy(() -> new PersistedTenant(tenant, state))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_mismatched_tenant_name() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = tenant(tenantId, "Acme");
        TenantState state = state(tenantId, "Globex");

        assertThatThrownBy(() -> new PersistedTenant(tenant, state))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static Tenant tenant(UUID id, String name) {
        return Tenant.reconstitute(new TenantSnapshot(
                id,
                new TenantName(name),
                TenantStatus.ACTIVE,
                CREATED_AT,
                UPDATED_AT
        ));
    }

    private static TenantState state(UUID id, String name) {
        return new TenantState(
                id,
                name,
                TenantStatus.ACTIVE,
                CREATED_AT,
                UPDATED_AT,
                0L
        );
    }
}
