package com.polarishb.pabal.tenant.contract.persistence;

import com.polarishb.pabal.tenant.domain.model.TenantRegistration;
import com.polarishb.pabal.tenant.domain.model.type.TenantRegistrationStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contract under test: {@code TenantRegistrationState} carries both
 * {@code verificationExpiresAt} and {@code activationExpiresAt} (no single
 * {@code expiresAt}), and round-trips through {@code TenantRegistration.snapshot()} /
 * {@code TenantRegistrationPersistenceMapper}.
 */
class TenantRegistrationStateTest {

    private static final String TOKEN = "abcdefghijklmnopqrstuvwxyzABCDEF";

    @Test
    void toState_and_toDomain_round_trip_both_expiry_fields_for_domain_verified_registration() {
        Instant requestedAt = Instant.parse("2026-06-19T00:00:00Z");
        Instant verificationExpiresAt = requestedAt.plusSeconds(60);
        Instant verifiedAt = requestedAt.plusSeconds(1);
        Instant activationExpiresAt = verifiedAt.plusSeconds(3600);

        TenantRegistration registration = TenantRegistration.request(
                "Acme",
                "example.com",
                TOKEN,
                requestedAt,
                verificationExpiresAt
        ).markVerified(verifiedAt, activationExpiresAt);

        TenantRegistrationState state = TenantRegistrationPersistenceMapper.toState(registration, 1L);

        assertThat(state.verificationExpiresAt()).isEqualTo(verificationExpiresAt);
        assertThat(state.activationExpiresAt()).isEqualTo(activationExpiresAt);
        assertThat(state.status()).isEqualTo(TenantRegistrationStatus.DOMAIN_VERIFIED);

        TenantRegistration reconstructed = TenantRegistrationPersistenceMapper.toDomain(state);

        assertThat(reconstructed.getVerificationExpiresAt()).isEqualTo(verificationExpiresAt);
        assertThat(reconstructed.getActivationExpiresAt()).isEqualTo(activationExpiresAt);
        assertThat(reconstructed.getStatus()).isEqualTo(TenantRegistrationStatus.DOMAIN_VERIFIED);
        assertThat(reconstructed.getVerifiedAt()).isEqualTo(verifiedAt);
    }

    @Test
    void toPersisted_preserves_both_expiry_fields_on_the_wrapped_state() {
        Instant requestedAt = Instant.parse("2026-06-19T00:00:00Z");
        Instant verificationExpiresAt = requestedAt.plusSeconds(60);

        TenantRegistration registration = TenantRegistration.request(
                "Acme",
                "example.com",
                TOKEN,
                requestedAt,
                verificationExpiresAt
        );

        TenantRegistrationState state = TenantRegistrationPersistenceMapper.toState(registration, null);
        PersistedTenantRegistration persisted = TenantRegistrationPersistenceMapper.toPersisted(state);

        assertThat(persisted.state().verificationExpiresAt()).isEqualTo(verificationExpiresAt);
        assertThat(persisted.state().activationExpiresAt()).isNull();
        assertThat(persisted.registration().getVerificationExpiresAt()).isEqualTo(verificationExpiresAt);
    }

    @Test
    void withRegistration_reflects_activationExpiresAt_set_by_markVerified_transition() {
        Instant requestedAt = Instant.parse("2026-06-19T00:00:00Z");
        Instant verificationExpiresAt = requestedAt.plusSeconds(60);
        Instant verifiedAt = requestedAt.plusSeconds(1);
        Instant activationExpiresAt = verifiedAt.plusSeconds(3600);

        TenantRegistration registration = TenantRegistration.request(
                "Acme",
                "example.com",
                TOKEN,
                requestedAt,
                verificationExpiresAt
        );

        TenantRegistrationState state = TenantRegistrationPersistenceMapper.toState(registration, 0L);
        PersistedTenantRegistration persisted = TenantRegistrationPersistenceMapper.toPersisted(state);

        TenantRegistration verified = persisted.registration().markVerified(verifiedAt, activationExpiresAt);
        PersistedTenantRegistration updated = persisted.withRegistration(verified);

        assertThat(updated.registration().getActivationExpiresAt()).isEqualTo(activationExpiresAt);
        // the persisted "state" snapshot itself is unchanged until a fresh toState() call
        assertThat(updated.state().activationExpiresAt()).isNull();
    }
}
