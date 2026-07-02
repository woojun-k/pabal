package com.polarishb.pabal.tenant.application.command.handler;

import com.polarishb.pabal.tenant.application.command.input.ExpireTenantRegistrationsCommand;
import com.polarishb.pabal.tenant.application.port.out.persistence.TenantRegistrationRepository;
import com.polarishb.pabal.tenant.application.port.out.time.ClockPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Contract under test (ADR-0013 follow-up, grace-window terminal expiry): the
 * expiration sweep use case is extended so lapsed REVERIFICATION_REQUIRED registrations
 * become EXPIRED after a grace window
 * (activationExpiresAt + reverification-grace-ms &lt;= now). Time is obtained only via
 * ClockPort.
 *
 * <p><b>Naming ambiguity (flagged, not guessed):</b> the contract explicitly leaves the
 * repository port method name for this bulk grace-expiry open ("whatever additional
 * bulk-expiry/query method(s) the grace sweep needs"). This unit test therefore only
 * asserts the handler's aggregate return-value contract (sum of pending-expiry count and
 * grace-expiry count) via the existing, contract-fixed
 * {@code expirePendingRegistrations} port method plus ClockPort usage; it does not
 * assert a specific new port method name or call count, since that name is not fixed by
 * the contract text. See the test-writer session report for the full flag and the
 * infra-level tests that do exercise a concrete (mechanically-named, not
 * contract-guaranteed) method.
 */
class ExpireTenantRegistrationsCommandHandlerTest {

    private static final Instant NOW = Instant.parse("2026-06-19T00:00:00Z");

    private final TenantRegistrationRepository tenantRegistrationRepository = mock(TenantRegistrationRepository.class);
    private final ClockPort clockPort = mock(ClockPort.class);

    private ExpireTenantRegistrationsCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ExpireTenantRegistrationsCommandHandler(tenantRegistrationRepository, clockPort);
    }

    @Test
    void handle_uses_clockPort_now_not_wall_clock_for_the_pending_expiry_sweep() {
        when(clockPort.now()).thenReturn(NOW);
        when(tenantRegistrationRepository.expirePendingRegistrations(NOW)).thenReturn(0);

        handler.handle(new ExpireTenantRegistrationsCommand());

        org.mockito.Mockito.verify(tenantRegistrationRepository).expirePendingRegistrations(NOW);
    }

    @Test
    void handle_still_returns_the_pending_expiry_count_when_no_lapsed_reverification_rows_exist() {
        when(clockPort.now()).thenReturn(NOW);
        when(tenantRegistrationRepository.expirePendingRegistrations(NOW)).thenReturn(3);

        Integer expiredCount = handler.handle(new ExpireTenantRegistrationsCommand());

        assertThat(expiredCount).isGreaterThanOrEqualTo(3);
    }
}
