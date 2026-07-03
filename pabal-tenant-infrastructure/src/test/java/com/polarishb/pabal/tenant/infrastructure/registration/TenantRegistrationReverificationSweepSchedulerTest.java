package com.polarishb.pabal.tenant.infrastructure.registration;

import com.polarishb.pabal.support.AbstractTenantPostgresDataJpaTest;
import com.polarishb.pabal.tenant.application.command.handler.ExpireTenantRegistrationsCommandHandler;
import com.polarishb.pabal.tenant.application.command.handler.ReverifyLapsedTenantRegistrationsCommandHandler;
import com.polarishb.pabal.tenant.application.port.out.persistence.TenantRegistrationRepository;
import com.polarishb.pabal.tenant.application.port.out.time.ClockPort;
import com.polarishb.pabal.tenant.contract.persistence.PersistedTenantRegistration;
import com.polarishb.pabal.tenant.contract.persistence.TenantRegistrationPersistenceMapper;
import com.polarishb.pabal.tenant.domain.model.TenantRegistration;
import com.polarishb.pabal.tenant.domain.model.type.TenantRegistrationStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contract under test: the {@code reverifyLapsedRegistrations()} method on
 * {@link TenantRegistrationExpirationScheduler} invokes the reverification-sweep
 * handler on a configurable fixed delay
 * ({@code pabal.tenant.registration.reverification-sweep-delay-ms}, default 600000).
 */
@Import({
        ExpireTenantRegistrationsCommandHandler.class,
        ReverifyLapsedTenantRegistrationsCommandHandler.class,
        TenantRegistrationExpirationScheduler.class,
        TenantRegistrationReverificationSweepSchedulerTest.TestPorts.class
})
@TestPropertySource(properties = {
        "pabal.tenant.registration.expiration-sweep-delay-ms=600000",
        "pabal.tenant.registration.reverification-sweep-delay-ms=600000",
        "pabal.tenant.registration.reverification-grace-ms=604800000"
})
class TenantRegistrationReverificationSweepSchedulerTest extends AbstractTenantPostgresDataJpaTest {

    @Autowired
    private TenantRegistrationExpirationScheduler scheduler;

    @Autowired
    private TenantRegistrationRepository tenantRegistrationRepository;

    @Autowired
    private MutableClockPort clockPort;

    @Autowired
    private EntityManager entityManager;

    @Test
    void reverifyLapsedRegistrations_transitions_lapsed_domain_verified_rows_through_the_handler() {
        Instant now = Instant.parse("2026-06-19T00:00:00Z");
        clockPort.setNow(now);

        TenantRegistration lapsed = TenantRegistration.request(
                "Acme",
                "scheduler-sweep-lapsed.example.com",
                "abcdefghijklmnopqrstuvwxyzABCDEF",
                now.minus(Duration.ofDays(10)),
                now.minus(Duration.ofDays(9)).plusSeconds(1)
        ).markVerified(now.minus(Duration.ofDays(9)), now.minus(Duration.ofDays(1)));
        PersistedTenantRegistration savedLapsed = tenantRegistrationRepository.append(
                new PersistedTenantRegistration(
                        lapsed,
                        TenantRegistrationPersistenceMapper.toState(lapsed, null)
                )
        );
        entityManager.flush();
        entityManager.clear();

        scheduler.reverifyLapsedRegistrations();

        entityManager.flush();
        entityManager.clear();
        PersistedTenantRegistration found = tenantRegistrationRepository.findById(
                savedLapsed.state().id()
        ).orElseThrow();
        assertThat(found.state().status()).isEqualTo(TenantRegistrationStatus.REVERIFICATION_REQUIRED);
    }

    /**
     * Contract under test (grace-window terminal expiry): the expiration sweep on
     * {@code TenantRegistrationExpirationScheduler} transitions a
     * REVERIFICATION_REQUIRED row whose
     * {@code activationExpiresAt + reverification-grace-ms <= now} to EXPIRED, while a
     * REVERIFICATION_REQUIRED row still inside the grace window is untouched. The
     * default grace is {@code 604800000} ms (7 days), wired here via
     * {@code pabal.tenant.registration.reverification-grace-ms} on the class-level
     * {@code @TestPropertySource}. The test runs both scheduled methods and asserts on
     * the resulting persisted status, independent of which method performs the sweep.
     */
    @Test
    void expiration_sweep_terminally_expires_reverification_required_rows_past_the_grace_window_but_not_rows_within_it() {
        Instant now = Instant.parse("2026-06-19T00:00:00Z");
        clockPort.setNow(now);

        TenantRegistration pastGrace = TenantRegistration.request(
                "Acme",
                "grace-sweep-past.example.com",
                "abcdefghijklmnopqrstuvwxyzABCDEF",
                now.minus(Duration.ofDays(30)),
                now.minus(Duration.ofDays(29)).plusSeconds(1)
        ).markVerified(now.minus(Duration.ofDays(29)), now.minus(Duration.ofDays(29)).plusSeconds(1))
                .requireReverification(now.minus(Duration.ofDays(8)));
        // Note: activationExpiresAt (not the later requireReverification() sweep time) is
        // the anchor for the grace cutoff (activationExpiresAt + grace-ms <= now), per the
        // contract text and the matching TenantRegistrationRepositoryImplTest fixtures.
        // This row's activationExpiresAt (now - 1 day) is only 1 day stale, so its cutoff
        // (now - 1 day + 7 days = now + 6 days) is still in the future - genuinely within
        // the 7-day grace window, unlike pastGrace's 29-day-stale activationExpiresAt above.
        TenantRegistration withinGrace = TenantRegistration.request(
                "Beta",
                "grace-sweep-within.example.com",
                "ABCDEFabcdefghijklmnopqrstuvwxyz",
                now.minus(Duration.ofDays(3)),
                now.minus(Duration.ofDays(2)).plusSeconds(1)
        ).markVerified(now.minus(Duration.ofDays(2)), now.minus(Duration.ofDays(1)))
                .requireReverification(now.minus(Duration.ofHours(12)));

        PersistedTenantRegistration savedPastGrace = tenantRegistrationRepository.append(
                new PersistedTenantRegistration(
                        pastGrace,
                        TenantRegistrationPersistenceMapper.toState(pastGrace, null)
                )
        );
        PersistedTenantRegistration savedWithinGrace = tenantRegistrationRepository.append(
                new PersistedTenantRegistration(
                        withinGrace,
                        TenantRegistrationPersistenceMapper.toState(withinGrace, null)
                )
        );
        entityManager.flush();
        entityManager.clear();

        scheduler.expirePendingRegistrations();
        scheduler.reverifyLapsedRegistrations();

        entityManager.flush();
        entityManager.clear();
        PersistedTenantRegistration foundPastGrace = tenantRegistrationRepository.findById(
                savedPastGrace.state().id()
        ).orElseThrow();
        PersistedTenantRegistration foundWithinGrace = tenantRegistrationRepository.findById(
                savedWithinGrace.state().id()
        ).orElseThrow();

        assertThat(foundPastGrace.state().status()).isEqualTo(TenantRegistrationStatus.EXPIRED);
        assertThat(foundWithinGrace.state().status()).isEqualTo(TenantRegistrationStatus.REVERIFICATION_REQUIRED);
    }

    @TestConfiguration
    static class TestPorts {
        @Bean
        MutableClockPort clockPort() {
            return new MutableClockPort();
        }
    }

    static class MutableClockPort implements ClockPort {
        private Instant now = Instant.parse("2026-06-19T00:00:00Z");

        void setNow(Instant now) {
            this.now = Objects.requireNonNull(now, "now must not be null");
        }

        @Override
        public Instant now() {
            return now;
        }
    }
}
