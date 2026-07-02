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

    /**
     * Contract under test: {@code TenantRegistrationEntity} round-trips both
     * {@code verificationExpiresAt} and {@code activationExpiresAt} independently of the
     * base-entity created/updated timestamps. Covers the PENDING_VERIFICATION
     * (null activation) shape exactly as persisted.
     */
    @Test
    void tenantRegistration_fromNewState_toState_round_trips_verificationExpiresAt_with_null_activationExpiresAt() {
        Instant verificationExpiresAt = CREATED_AT.plusSeconds(3600);
        TenantRegistrationState state = new TenantRegistrationState(
                UUID.randomUUID(),
                "Acme",
                "example.com",
                "abcdefghijklmnopqrstuvwxyzABCDEF",
                TenantRegistrationStatus.PENDING_VERIFICATION,
                verificationExpiresAt,
                null,
                null,
                null,
                null,
                CREATED_AT,
                UPDATED_AT,
                null
        );

        TenantRegistrationEntity entity = TenantRegistrationEntity.fromNewState(state);

        assertThat(entity.toState().verificationExpiresAt()).isEqualTo(verificationExpiresAt);
        assertThat(entity.toState().activationExpiresAt()).isNull();
    }

    /**
     * Contract under test: {@code TenantRegistrationEntity} round-trips a non-null
     * {@code activationExpiresAt} for a DOMAIN_VERIFIED registration.
     */
    @Test
    void tenantRegistration_fromNewState_toState_round_trips_non_null_activationExpiresAt() {
        Instant verificationExpiresAt = CREATED_AT.plusSeconds(3600);
        Instant verifiedAt = CREATED_AT.plusSeconds(30);
        Instant activationExpiresAt = verifiedAt.plusSeconds(3600);
        TenantRegistrationState state = new TenantRegistrationState(
                UUID.randomUUID(),
                "Acme",
                "example.com",
                "abcdefghijklmnopqrstuvwxyzABCDEF",
                TenantRegistrationStatus.DOMAIN_VERIFIED,
                verificationExpiresAt,
                activationExpiresAt,
                verifiedAt,
                null,
                null,
                CREATED_AT,
                UPDATED_AT,
                null
        );

        TenantRegistrationEntity entity = TenantRegistrationEntity.fromNewState(state);

        assertThat(entity.toState().verificationExpiresAt()).isEqualTo(verificationExpiresAt);
        assertThat(entity.toState().activationExpiresAt()).isEqualTo(activationExpiresAt);
        assertThat(entity.toState().status()).isEqualTo(TenantRegistrationStatus.DOMAIN_VERIFIED);
    }

    /**
     * Contract under test: {@code TenantRegistrationEntity#apply} also preserves both
     * timestamps on an update-in-place path (mirrors {@code TenantRegistrationRepositoryImpl#update}).
     */
    @Test
    void tenantRegistration_apply_preserves_both_expiry_fields_on_transition() {
        Instant verificationExpiresAt = CREATED_AT.plusSeconds(3600);
        TenantRegistrationState initialState = new TenantRegistrationState(
                UUID.randomUUID(),
                "Acme",
                "example.com",
                "abcdefghijklmnopqrstuvwxyzABCDEF",
                TenantRegistrationStatus.PENDING_VERIFICATION,
                verificationExpiresAt,
                null,
                null,
                null,
                null,
                CREATED_AT,
                UPDATED_AT,
                0L
        );
        TenantRegistrationEntity entity = TenantRegistrationEntity.fromNewState(initialState);

        Instant verifiedAt = CREATED_AT.plusSeconds(30);
        Instant activationExpiresAt = verifiedAt.plusSeconds(3600);
        TenantRegistrationState verifiedState = new TenantRegistrationState(
                initialState.id(),
                "Acme",
                "example.com",
                "abcdefghijklmnopqrstuvwxyzABCDEF",
                TenantRegistrationStatus.DOMAIN_VERIFIED,
                verificationExpiresAt,
                activationExpiresAt,
                verifiedAt,
                null,
                null,
                CREATED_AT,
                verifiedAt,
                0L
        );

        entity.apply(verifiedState);

        assertThat(entity.toState().verificationExpiresAt()).isEqualTo(verificationExpiresAt);
        assertThat(entity.toState().activationExpiresAt()).isEqualTo(activationExpiresAt);
        assertThat(entity.toState().status()).isEqualTo(TenantRegistrationStatus.DOMAIN_VERIFIED);
    }
}
