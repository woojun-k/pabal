package com.polarishb.pabal.tenant.application.command.handler;

import com.polarishb.pabal.tenant.application.command.input.ReverifyLapsedTenantRegistrationsCommand;
import com.polarishb.pabal.tenant.application.port.out.persistence.TenantRegistrationRepository;
import com.polarishb.pabal.tenant.application.port.out.time.ClockPort;
import com.polarishb.pabal.tenant.contract.persistence.PersistedTenantRegistration;
import com.polarishb.pabal.tenant.contract.persistence.TenantRegistrationPersistenceMapper;
import com.polarishb.pabal.tenant.contract.persistence.TenantRegistrationState;
import com.polarishb.pabal.tenant.domain.model.type.TenantRegistrationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Contract under test ("Restore compilation of the tenant context after the ADR-0013
 * domain/contract split..." - reverification sweep):
 *
 * <ul>
 *   <li>{@code now} is obtained from {@code ClockPort.now()}, never from
 *       {@code Instant.now()}.</li>
 *   <li>Candidates come from {@code TenantRegistrationRepository.findLapsedDomainVerifiedIds(now, limit)}.</li>
 *   <li>Each candidate is locked ({@code findByIdForUpdate}) and transitioned
 *       <em>only through the domain method</em> {@code requireReverification(now)} - no
 *       bulk JPQL update bypassing the domain.</li>
 *   <li>The handler returns the count of rows actually transitioned to
 *       {@code REVERIFICATION_REQUIRED}.</li>
 *   <li>Per-row resilience: if a candidate row concurrently changed so that
 *       {@code requireReverification} is no longer valid (e.g. status is no longer
 *       DOMAIN_VERIFIED, throwing {@code TenantRegistrationNotPendingException}), that row
 *       is skipped, not counted, and does not abort the remaining sweep.</li>
 * </ul>
 *
 * <p>The exact handler/command class names ({@code ReverifyLapsedTenantRegistrationsCommand}
 * / {@code ReverifyLapsedTenantRegistrationsCommandHandler}) and the port method name
 * ({@code findLapsedDomainVerifiedIds}) are not fixed by the contract text; they were
 * derived mechanically from the existing {@code ExpireTenantRegistrationsCommand} /
 * {@code ExpireTenantRegistrationsCommandHandler} / {@code findPendingVerificationIds}
 * naming convention. See the test-writer session report for this flagged ambiguity.
 */
class ReverifyLapsedTenantRegistrationsCommandHandlerTest {

    private static final Instant NOW = Instant.parse("2026-06-19T00:00:00Z");
    private static final int DEFAULT_LIMIT = 100;

    private final TenantRegistrationRepository tenantRegistrationRepository = mock(TenantRegistrationRepository.class);
    private final ClockPort clockPort = mock(ClockPort.class);

    private ReverifyLapsedTenantRegistrationsCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ReverifyLapsedTenantRegistrationsCommandHandler(
                tenantRegistrationRepository,
                clockPort
        );
    }

    @Test
    void handle_transitions_each_lapsed_domain_verified_candidate_through_domain_and_returns_count() {
        UUID firstId = UUID.randomUUID();
        UUID secondId = UUID.randomUUID();
        when(clockPort.now()).thenReturn(NOW);
        when(tenantRegistrationRepository.findLapsedDomainVerifiedIds(eq(NOW), anyInt()))
                .thenReturn(List.of(firstId, secondId));
        when(tenantRegistrationRepository.findByIdForUpdate(firstId))
                .thenReturn(Optional.of(lapsedDomainVerifiedRegistration(firstId, 3L)));
        when(tenantRegistrationRepository.findByIdForUpdate(secondId))
                .thenReturn(Optional.of(lapsedDomainVerifiedRegistration(secondId, 9L)));
        when(tenantRegistrationRepository.update(any(PersistedTenantRegistration.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Integer transitionedCount = handler.handle(new ReverifyLapsedTenantRegistrationsCommand());

        assertThat(transitionedCount).isEqualTo(2);

        ArgumentCaptor<PersistedTenantRegistration> captor = ArgumentCaptor.forClass(PersistedTenantRegistration.class);
        verify(tenantRegistrationRepository, times(2)).update(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(persisted -> persisted.registration().getStatus())
                .containsExactly(
                        TenantRegistrationStatus.REVERIFICATION_REQUIRED,
                        TenantRegistrationStatus.REVERIFICATION_REQUIRED
                );
        assertThat(captor.getAllValues())
                .extracting(persisted -> persisted.registration().getUpdatedAt())
                .containsOnly(NOW);
    }

    @Test
    void handle_uses_clockPort_now_not_wall_clock_for_the_lapsed_id_query() {
        when(clockPort.now()).thenReturn(NOW);
        when(tenantRegistrationRepository.findLapsedDomainVerifiedIds(any(), anyInt()))
                .thenReturn(List.of());

        handler.handle(new ReverifyLapsedTenantRegistrationsCommand());

        verify(tenantRegistrationRepository).findLapsedDomainVerifiedIds(eq(NOW), anyInt());
    }

    @Test
    void handle_returns_zero_when_no_candidates_are_lapsed() {
        when(clockPort.now()).thenReturn(NOW);
        when(tenantRegistrationRepository.findLapsedDomainVerifiedIds(eq(NOW), anyInt()))
                .thenReturn(List.of());

        Integer transitionedCount = handler.handle(new ReverifyLapsedTenantRegistrationsCommand());

        assertThat(transitionedCount).isZero();
        verify(tenantRegistrationRepository, never()).update(any(PersistedTenantRegistration.class));
    }

    /**
     * Negative/resilience test: a row that changed concurrently (no longer
     * DOMAIN_VERIFIED) must not abort the sweep and must not be counted as transitioned.
     * This is the security/concurrency invariant this contract explicitly requires:
     * "resilient to per-row failure".
     */
    @Test
    void handle_skips_candidate_row_that_is_no_longer_domain_verified_without_aborting_the_sweep() {
        UUID staleId = UUID.randomUUID();
        UUID healthyId = UUID.randomUUID();
        when(clockPort.now()).thenReturn(NOW);
        when(tenantRegistrationRepository.findLapsedDomainVerifiedIds(eq(NOW), anyInt()))
                .thenReturn(List.of(staleId, healthyId));
        // staleId's row concurrently advanced past DOMAIN_VERIFIED (e.g. someone already
        // reverified/activated it) -> requireReverification(now) throws
        // TenantRegistrationNotPendingException per domain contract.
        when(tenantRegistrationRepository.findByIdForUpdate(staleId))
                .thenReturn(Optional.of(activatedRegistration(staleId, 1L)));
        when(tenantRegistrationRepository.findByIdForUpdate(healthyId))
                .thenReturn(Optional.of(lapsedDomainVerifiedRegistration(healthyId, 2L)));
        when(tenantRegistrationRepository.update(any(PersistedTenantRegistration.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Integer transitionedCount = handler.handle(new ReverifyLapsedTenantRegistrationsCommand());

        assertThat(transitionedCount).isEqualTo(1);

        ArgumentCaptor<PersistedTenantRegistration> captor = ArgumentCaptor.forClass(PersistedTenantRegistration.class);
        verify(tenantRegistrationRepository, times(1)).update(captor.capture());
        assertThat(captor.getValue().state().id()).isEqualTo(healthyId);
    }

    /**
     * Negative/resilience test: if the activation window was actually still open when
     * the row is (re-)examined (a scheduler/query mismatch), {@code requireReverification}
     * throws {@code IllegalStateException} per the domain contract; the sweep must not
     * abort and must not count that row.
     */
    @Test
    void handle_skips_candidate_row_whose_activation_window_is_no_longer_lapsed_without_aborting_the_sweep() {
        UUID notYetLapsedId = UUID.randomUUID();
        UUID lapsedId = UUID.randomUUID();
        when(clockPort.now()).thenReturn(NOW);
        when(tenantRegistrationRepository.findLapsedDomainVerifiedIds(eq(NOW), anyInt()))
                .thenReturn(List.of(notYetLapsedId, lapsedId));
        when(tenantRegistrationRepository.findByIdForUpdate(notYetLapsedId))
                .thenReturn(Optional.of(domainVerifiedRegistrationWithActivationExpiresAt(
                        notYetLapsedId, 4L, NOW.plusSeconds(60)
                )));
        when(tenantRegistrationRepository.findByIdForUpdate(lapsedId))
                .thenReturn(Optional.of(lapsedDomainVerifiedRegistration(lapsedId, 5L)));
        when(tenantRegistrationRepository.update(any(PersistedTenantRegistration.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Integer transitionedCount = handler.handle(new ReverifyLapsedTenantRegistrationsCommand());

        assertThat(transitionedCount).isEqualTo(1);
        verify(tenantRegistrationRepository, times(1)).update(any(PersistedTenantRegistration.class));
    }

    /**
     * Static/behavioral proof that the transition goes through the domain, not a bulk
     * update: the persisted registration handed to {@code update} carries the domain
     * object whose status transition can only have come from
     * {@code TenantRegistration.requireReverification}, since a row starting as
     * DOMAIN_VERIFIED with an already-lapsed status (simulated here as already
     * NOT DOMAIN_VERIFIED) is rejected with {@code TenantRegistrationNotPendingException}
     * rather than silently coerced to REVERIFICATION_REQUIRED.
     */
    @Test
    void handle_rejects_non_domain_verified_row_via_domain_exception_rather_than_forcing_status() {
        UUID pendingId = UUID.randomUUID();
        when(clockPort.now()).thenReturn(NOW);
        when(tenantRegistrationRepository.findLapsedDomainVerifiedIds(eq(NOW), anyInt()))
                .thenReturn(List.of(pendingId));
        when(tenantRegistrationRepository.findByIdForUpdate(pendingId))
                .thenReturn(Optional.of(pendingRegistration(pendingId, 0L)));

        Integer transitionedCount = handler.handle(new ReverifyLapsedTenantRegistrationsCommand());

        assertThat(transitionedCount).isZero();
        verify(tenantRegistrationRepository, never()).update(any(PersistedTenantRegistration.class));
    }

    private PersistedTenantRegistration lapsedDomainVerifiedRegistration(UUID registrationId, Long version) {
        Instant requestedAt = NOW.minus(java.time.Duration.ofDays(10));
        Instant verifiedAt = requestedAt.plusSeconds(30);
        Instant activationExpiresAt = NOW.minus(java.time.Duration.ofDays(1));
        TenantRegistrationState state = new TenantRegistrationState(
                registrationId,
                "Acme",
                "example.com",
                "abcdefghijklmnopqrstuvwxyzABCDEF",
                TenantRegistrationStatus.DOMAIN_VERIFIED,
                requestedAt.plusSeconds(3600),
                activationExpiresAt,
                verifiedAt,
                null,
                null,
                requestedAt,
                verifiedAt,
                version
        );
        return TenantRegistrationPersistenceMapper.toPersisted(state);
    }

    private PersistedTenantRegistration domainVerifiedRegistrationWithActivationExpiresAt(
            UUID registrationId,
            Long version,
            Instant activationExpiresAt
    ) {
        Instant requestedAt = NOW.minus(java.time.Duration.ofDays(1));
        Instant verifiedAt = requestedAt.plusSeconds(30);
        TenantRegistrationState state = new TenantRegistrationState(
                registrationId,
                "Acme",
                "example.com",
                "abcdefghijklmnopqrstuvwxyzABCDEF",
                TenantRegistrationStatus.DOMAIN_VERIFIED,
                requestedAt.plusSeconds(3600),
                activationExpiresAt,
                verifiedAt,
                null,
                null,
                requestedAt,
                verifiedAt,
                version
        );
        return TenantRegistrationPersistenceMapper.toPersisted(state);
    }

    private PersistedTenantRegistration activatedRegistration(UUID registrationId, Long version) {
        Instant requestedAt = NOW.minus(java.time.Duration.ofDays(10));
        Instant verifiedAt = requestedAt.plusSeconds(30);
        Instant activationExpiresAt = NOW.minus(java.time.Duration.ofDays(1));
        Instant activatedAt = requestedAt.plusSeconds(60);
        TenantRegistrationState state = new TenantRegistrationState(
                registrationId,
                "Acme",
                "example.com",
                "abcdefghijklmnopqrstuvwxyzABCDEF",
                TenantRegistrationStatus.ACTIVATED,
                requestedAt.plusSeconds(3600),
                activationExpiresAt,
                verifiedAt,
                activatedAt,
                UUID.randomUUID(),
                requestedAt,
                activatedAt,
                version
        );
        return TenantRegistrationPersistenceMapper.toPersisted(state);
    }

    private PersistedTenantRegistration pendingRegistration(UUID registrationId, Long version) {
        Instant requestedAt = NOW.minus(java.time.Duration.ofDays(1));
        TenantRegistrationState state = new TenantRegistrationState(
                registrationId,
                "Acme",
                "example.com",
                "abcdefghijklmnopqrstuvwxyzABCDEF",
                TenantRegistrationStatus.PENDING_VERIFICATION,
                requestedAt.plusSeconds(3600),
                null,
                null,
                null,
                null,
                requestedAt,
                requestedAt,
                version
        );
        return TenantRegistrationPersistenceMapper.toPersisted(state);
    }
}
