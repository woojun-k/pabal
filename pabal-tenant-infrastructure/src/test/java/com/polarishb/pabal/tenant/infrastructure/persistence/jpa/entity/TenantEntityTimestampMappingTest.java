package com.polarishb.pabal.tenant.infrastructure.persistence.jpa.entity;

import com.polarishb.pabal.tenant.contract.persistence.TenantRegistrationState;
import com.polarishb.pabal.tenant.contract.persistence.TenantState;
import com.polarishb.pabal.tenant.domain.model.type.TenantRegistrationStatus;
import com.polarishb.pabal.tenant.domain.model.type.TenantStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TenantEntityTimestampMappingTest {

    private static final Instant CREATED_AT = Instant.parse("2026-06-19T00:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-06-19T01:00:00Z");

    @Test
    void tenant_fromNewState_maps_contract_timestamps_to_base_entity_fields() {
        TenantState state = new TenantState(
                UUID.randomUUID(),
                "Acme",
                TenantStatus.ACTIVE,
                CREATED_AT,
                UPDATED_AT,
                null
        );

        TenantEntity entity = TenantEntity.fromNewState(state);

        assertThat(entity.getCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(entity.getUpdatedAt()).isEqualTo(UPDATED_AT);
        assertThat(entity.toState().createdAt()).isEqualTo(CREATED_AT);
        assertThat(entity.toState().updatedAt()).isEqualTo(UPDATED_AT);
    }

    @Test
    void tenantRegistration_fromNewState_maps_contract_timestamps_to_base_entity_fields() {
        TenantRegistrationState state = new TenantRegistrationState(
                UUID.randomUUID(),
                "Acme",
                "example.com",
                "abcdefghijklmnopqrstuvwxyzABCDEF",
                TenantRegistrationStatus.PENDING_VERIFICATION,
                CREATED_AT.plusSeconds(3600),
                null,
                null,
                null,
                CREATED_AT,
                UPDATED_AT,
                null
        );

        TenantRegistrationEntity entity = TenantRegistrationEntity.fromNewState(state);

        assertThat(entity.getCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(entity.getUpdatedAt()).isEqualTo(UPDATED_AT);
        assertThat(entity.toState().createdAt()).isEqualTo(CREATED_AT);
        assertThat(entity.toState().updatedAt()).isEqualTo(UPDATED_AT);
    }
}
