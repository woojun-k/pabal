package com.polarishb.pabal.user.infrastructure.persistence.jpa.entity;

import com.polarishb.pabal.user.contract.persistence.UserState;
import com.polarishb.pabal.user.domain.model.type.UserStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TenantUserEntityTimestampMappingTest {

    private static final Instant CREATED_AT = Instant.parse("2026-06-19T00:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-06-19T01:00:00Z");

    @Test
    void fromState_maps_contract_timestamps_to_base_entity_fields() {
        UserState state = new UserState(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Alice",
                UserStatus.ACTIVE,
                CREATED_AT,
                UPDATED_AT,
                null
        );

        TenantUserEntity entity = TenantUserEntity.fromState(state);

        assertThat(entity.getCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(entity.getUpdatedAt()).isEqualTo(UPDATED_AT);
        assertThat(entity.toState().createdAt()).isEqualTo(CREATED_AT);
        assertThat(entity.toState().updatedAt()).isEqualTo(UPDATED_AT);
    }
}
