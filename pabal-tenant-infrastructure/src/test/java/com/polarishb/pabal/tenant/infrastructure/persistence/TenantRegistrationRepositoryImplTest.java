package com.polarishb.pabal.tenant.infrastructure.persistence;

import com.polarishb.pabal.support.AbstractTenantPostgresDataJpaTest;
import com.polarishb.pabal.tenant.application.port.out.persistence.TenantRegistrationRepository;
import com.polarishb.pabal.tenant.contract.persistence.PersistedTenantRegistration;
import com.polarishb.pabal.tenant.contract.persistence.TenantRegistrationPersistenceMapper;
import com.polarishb.pabal.tenant.domain.model.TenantRegistration;
import com.polarishb.pabal.tenant.domain.model.type.TenantRegistrationStatus;
import com.polarishb.pabal.tenant.domain.model.vo.TenantDomainName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TenantRegistrationRepositoryImplTest extends AbstractTenantPostgresDataJpaTest {

    @Autowired
    private TenantRegistrationRepository tenantRegistrationRepository;

    @Test
    void append_and_findById_round_trips_pending_registration() {
        Instant now = Instant.parse("2026-06-19T00:00:00Z");
        TenantRegistration registration = TenantRegistration.request(
                "Acme",
                "Example.COM",
                "abcdefghijklmnopqrstuvwxyzABCDEF",
                now,
                now.plus(java.time.Duration.ofDays(7))
        );

        PersistedTenantRegistration saved = tenantRegistrationRepository.append(
                new PersistedTenantRegistration(
                        registration,
                        TenantRegistrationPersistenceMapper.toState(registration, null)
                )
        );
        Optional<PersistedTenantRegistration> found = tenantRegistrationRepository.findById(saved.state().id());

        assertThat(saved.state().id()).isNotNull();
        assertThat(saved.state().tenantName()).isEqualTo("Acme");
        assertThat(saved.state().domainName()).isEqualTo("example.com");
        assertThat(saved.state().version()).isEqualTo(0L);
        assertThat(found).isPresent();
        assertThat(found.orElseThrow().state().id()).isEqualTo(saved.state().id());
        assertThat(tenantRegistrationRepository.existsOpenByDomainName(new TenantDomainName("example.com"))).isTrue();
    }

    @Test
    void expirePendingRegistrations_marks_expired_pending_rows_as_expired() {
        Instant now = Instant.parse("2026-06-19T00:00:00Z");
        TenantRegistration registration = TenantRegistration.request(
                "Acme",
                "example.com",
                "abcdefghijklmnopqrstuvwxyzABCDEF",
                now.minus(java.time.Duration.ofDays(8)),
                now.minus(java.time.Duration.ofDays(1))
        );

        PersistedTenantRegistration saved = tenantRegistrationRepository.append(
                new PersistedTenantRegistration(
                        registration,
                        TenantRegistrationPersistenceMapper.toState(registration, null)
                )
        );

        int expiredCount = tenantRegistrationRepository.expirePendingRegistrations(now);
        PersistedTenantRegistration found = tenantRegistrationRepository.findById(saved.state().id()).orElseThrow();

        assertThat(expiredCount).isEqualTo(1);
        assertThat(found.state().status()).isEqualTo(TenantRegistrationStatus.EXPIRED);
        assertThat(tenantRegistrationRepository.existsOpenByDomainName(new TenantDomainName("example.com"))).isFalse();
    }

    /**
     * Contract: {@code expirePendingRegistrations} must not touch DOMAIN_VERIFIED or
     * REVERIFICATION_REQUIRED rows, even when their activation window has lapsed - those
     * are the reverification sweep's responsibility, not the pending-expiry sweep's.
     */
    @Test
    void expirePendingRegistrations_does_not_touch_lapsed_domain_verified_rows() {
        Instant now = Instant.parse("2026-06-19T00:00:00Z");
        PersistedTenantRegistration lapsedDomainVerified = saveDomainVerifiedRegistration(
                "Acme",
                "expire-sweep-domain-verified.example.com",
                now.minus(Duration.ofDays(10)),
                now.minus(Duration.ofDays(9)),
                now.minus(Duration.ofDays(1))
        );

        int expiredCount = tenantRegistrationRepository.expirePendingRegistrations(now);
        PersistedTenantRegistration found = tenantRegistrationRepository.findById(
                lapsedDomainVerified.state().id()
        ).orElseThrow();

        assertThat(expiredCount).isZero();
        assertThat(found.state().status()).isEqualTo(TenantRegistrationStatus.DOMAIN_VERIFIED);
    }

    @Test
    void findPendingVerificationIds_returns_unexpired_pending_rows() {
        Instant now = Instant.parse("2026-06-19T00:00:00Z");
        TenantRegistration registration = TenantRegistration.request(
                "Acme",
                "example.com",
                "abcdefghijklmnopqrstuvwxyzABCDEF",
                now,
                now.plus(java.time.Duration.ofDays(7))
        );
        PersistedTenantRegistration saved = tenantRegistrationRepository.append(
                new PersistedTenantRegistration(
                        registration,
                        TenantRegistrationPersistenceMapper.toState(registration, null)
                )
        );

        assertThat(tenantRegistrationRepository.findPendingVerificationIds(now, 10))
                .containsExactly(saved.state().id());
    }

    /**
     * Contract: {@code findPendingVerificationIds} is keyed on {@code verification_expires_at}
     * (not the removed {@code expires_at}); a row whose verification window has already
     * lapsed is excluded.
     */
    @Test
    void findPendingVerificationIds_excludes_rows_whose_verification_window_has_lapsed() {
        Instant now = Instant.parse("2026-06-19T00:00:00Z");
        TenantRegistration lapsed = TenantRegistration.request(
                "Acme",
                "lapsed-verification.example.com",
                "abcdefghijklmnopqrstuvwxyzABCDEF",
                now.minus(Duration.ofDays(10)),
                now.minus(Duration.ofDays(1))
        );
        tenantRegistrationRepository.append(
                new PersistedTenantRegistration(
                        lapsed,
                        TenantRegistrationPersistenceMapper.toState(lapsed, null)
                )
        );

        assertThat(tenantRegistrationRepository.findPendingVerificationIds(now, 10)).isEmpty();
    }

    /**
     * Contract: {@code existsOpenByDomainName} (backed by {@code OPEN_STATUSES}) must
     * treat DOMAIN_VERIFIED and REVERIFICATION_REQUIRED as open - the removed
     * {@code VERIFIED} status must no longer appear anywhere in the open-status set.
     */
    @Test
    void existsOpenByDomainName_treats_domain_verified_as_open() {
        Instant now = Instant.parse("2026-06-19T00:00:00Z");
        saveDomainVerifiedRegistration(
                "Acme",
                "open-domain-verified.example.com",
                now.minus(Duration.ofDays(1)),
                now,
                now.plus(Duration.ofDays(7))
        );

        assertThat(tenantRegistrationRepository.existsOpenByDomainName(
                new TenantDomainName("open-domain-verified.example.com")
        )).isTrue();
    }

    @Test
    void existsOpenByDomainName_treats_reverification_required_as_open() {
        Instant now = Instant.parse("2026-06-19T00:00:00Z");
        saveReverificationRequiredRegistration(
                "Acme",
                "open-reverification-required.example.com",
                now.minus(Duration.ofDays(2)),
                now.minus(Duration.ofDays(1)),
                now.minus(Duration.ofHours(1))
        );

        assertThat(tenantRegistrationRepository.existsOpenByDomainName(
                new TenantDomainName("open-reverification-required.example.com")
        )).isTrue();
    }

    /**
     * Contract: the reverification sweep's finder returns only DOMAIN_VERIFIED ids whose
     * activation window has lapsed (activation_expires_at <= now); a non-lapsed
     * DOMAIN_VERIFIED row and a PENDING_VERIFICATION row are excluded.
     */
    @Test
    void findLapsedDomainVerifiedIds_returns_only_lapsed_domain_verified_rows() {
        Instant now = Instant.parse("2026-06-19T00:00:00Z");

        PersistedTenantRegistration lapsed = saveDomainVerifiedRegistration(
                "Acme",
                "lapsed-activation.example.com",
                now.minus(Duration.ofDays(10)),
                now.minus(Duration.ofDays(9)),
                now.minus(Duration.ofDays(1))
        );
        saveDomainVerifiedRegistration(
                "Beta",
                "open-activation.example.com",
                now.minus(Duration.ofDays(1)),
                now,
                now.plus(Duration.ofDays(7))
        );
        TenantRegistration pending = TenantRegistration.request(
                "Gamma",
                "pending.example.com",
                "ABCDEFabcdefghijklmnopqrstuvwxyz",
                now,
                now.plus(Duration.ofDays(7))
        );
        tenantRegistrationRepository.append(
                new PersistedTenantRegistration(
                        pending,
                        TenantRegistrationPersistenceMapper.toState(pending, null)
                )
        );

        assertThat(tenantRegistrationRepository.findLapsedDomainVerifiedIds(now, 10))
                .containsExactly(lapsed.state().id());
    }

    @Test
    void findLapsedDomainVerifiedIds_includes_row_at_exact_activationExpiresAt_boundary() {
        Instant now = Instant.parse("2026-06-19T00:00:00Z");
        PersistedTenantRegistration atBoundary = saveDomainVerifiedRegistration(
                "Acme",
                "boundary-activation.example.com",
                now.minus(Duration.ofDays(2)),
                now.minus(Duration.ofDays(1)),
                now
        );

        assertThat(tenantRegistrationRepository.findLapsedDomainVerifiedIds(now, 10))
                .containsExactly(atBoundary.state().id());
    }

    /**
     * Contract under test (ADR-0013 follow-up, grace-window terminal expiry):
     * REVERIFICATION_REQUIRED rows are expired only once
     * {@code activationExpiresAt + reverification-grace-ms <= now}; a row still inside
     * the grace window is untouched.
     *
     * <p><b>Naming ambiguity (flagged, not guessed):</b> the contract leaves the exact
     * repository port method name for this bulk grace-expiry open. This test targets
     * {@code expireLapsedReverificationRegistrations(now, graceMs)}, mechanically derived
     * from the existing {@code expirePendingRegistrations(now)} verb/shape convention (a
     * bulk-UPDATE-returning-count method on the same port, mirroring how
     * {@code findLapsedDomainVerifiedIds} mirrors {@code findPendingVerificationIds}). If
     * the generator introduces a different method name/signature, this is a contract
     * defect to resolve, not a silent implementation choice - see the test-writer session
     * report.
     */
    @Test
    void expireLapsedReverificationRegistrations_expires_only_rows_past_the_grace_cutoff() {
        Instant now = Instant.parse("2026-06-19T00:00:00Z");
        long graceMs = Duration.ofDays(7).toMillis();

        PersistedTenantRegistration pastGrace = saveReverificationRequiredRegistration(
                "Acme",
                "grace-past.example.com",
                now.minus(Duration.ofDays(20)),
                now.minus(Duration.ofDays(19)),
                now.minus(Duration.ofDays(8))
        );
        PersistedTenantRegistration withinGrace = saveReverificationRequiredRegistration(
                "Beta",
                "grace-within.example.com",
                now.minus(Duration.ofDays(5)),
                now.minus(Duration.ofDays(4)),
                now.minus(Duration.ofDays(3))
        );

        int expiredCount = tenantRegistrationRepository.expireLapsedReverificationRegistrations(now, graceMs);

        PersistedTenantRegistration foundPastGrace = tenantRegistrationRepository.findById(
                pastGrace.state().id()
        ).orElseThrow();
        PersistedTenantRegistration foundWithinGrace = tenantRegistrationRepository.findById(
                withinGrace.state().id()
        ).orElseThrow();

        assertThat(expiredCount).isEqualTo(1);
        assertThat(foundPastGrace.state().status()).isEqualTo(TenantRegistrationStatus.EXPIRED);
        assertThat(foundWithinGrace.state().status()).isEqualTo(TenantRegistrationStatus.REVERIFICATION_REQUIRED);
    }

    /**
     * Boundary case: a row exactly at the grace cutoff
     * ({@code activationExpiresAt + reverification-grace-ms == now}) must be expired -
     * the contract's WHERE clause uses {@code <=}, matching the existing
     * {@code findLapsedDomainVerifiedIds} boundary convention.
     */
    @Test
    void expireLapsedReverificationRegistrations_includes_row_at_exact_grace_boundary() {
        Instant now = Instant.parse("2026-06-19T00:00:00Z");
        long graceMs = Duration.ofDays(7).toMillis();
        Instant activationExpiresAt = now.minus(Duration.ofDays(7));

        PersistedTenantRegistration atBoundary = saveReverificationRequiredRegistration(
                "Acme",
                "grace-boundary.example.com",
                now.minus(Duration.ofDays(20)),
                now.minus(Duration.ofDays(19)),
                activationExpiresAt
        );

        int expiredCount = tenantRegistrationRepository.expireLapsedReverificationRegistrations(now, graceMs);

        PersistedTenantRegistration found = tenantRegistrationRepository.findById(
                atBoundary.state().id()
        ).orElseThrow();

        assertThat(expiredCount).isEqualTo(1);
        assertThat(found.state().status()).isEqualTo(TenantRegistrationStatus.EXPIRED);
    }

    /**
     * Contract: DOMAIN_VERIFIED rows never transition directly to EXPIRED via this sweep
     * - they must go through the existing lapsed sweep to REVERIFICATION_REQUIRED first.
     */
    @Test
    void expireLapsedReverificationRegistrations_does_not_touch_domain_verified_rows() {
        Instant now = Instant.parse("2026-06-19T00:00:00Z");
        long graceMs = Duration.ofDays(7).toMillis();

        PersistedTenantRegistration domainVerified = saveDomainVerifiedRegistration(
                "Acme",
                "grace-domain-verified.example.com",
                now.minus(Duration.ofDays(30)),
                now.minus(Duration.ofDays(29)),
                now.minus(Duration.ofDays(20))
        );

        int expiredCount = tenantRegistrationRepository.expireLapsedReverificationRegistrations(now, graceMs);

        PersistedTenantRegistration found = tenantRegistrationRepository.findById(
                domainVerified.state().id()
        ).orElseThrow();

        assertThat(expiredCount).isZero();
        assertThat(found.state().status()).isEqualTo(TenantRegistrationStatus.DOMAIN_VERIFIED);
    }

    private PersistedTenantRegistration saveDomainVerifiedRegistration(
            String tenantName,
            String domainName,
            Instant requestedAt,
            Instant verifiedAt,
            Instant activationExpiresAt
    ) {
        TenantRegistration registration = TenantRegistration.request(
                tenantName,
                domainName,
                UUID.randomUUID().toString().replace("-", "").substring(0, 32),
                requestedAt,
                verifiedAt.plusSeconds(1)
        ).markVerified(verifiedAt, activationExpiresAt);
        return tenantRegistrationRepository.append(
                new PersistedTenantRegistration(
                        registration,
                        TenantRegistrationPersistenceMapper.toState(registration, null)
                )
        );
    }

    private PersistedTenantRegistration saveReverificationRequiredRegistration(
            String tenantName,
            String domainName,
            Instant requestedAt,
            Instant verifiedAt,
            Instant activationExpiresAt
    ) {
        TenantRegistration domainVerified = TenantRegistration.request(
                tenantName,
                domainName,
                UUID.randomUUID().toString().replace("-", "").substring(0, 32),
                requestedAt,
                verifiedAt.plusSeconds(1)
        ).markVerified(verifiedAt, activationExpiresAt);
        TenantRegistration lapsed = domainVerified.requireReverification(activationExpiresAt.plusSeconds(1));
        return tenantRegistrationRepository.append(
                new PersistedTenantRegistration(
                        lapsed,
                        TenantRegistrationPersistenceMapper.toState(lapsed, null)
                )
        );
    }
}
