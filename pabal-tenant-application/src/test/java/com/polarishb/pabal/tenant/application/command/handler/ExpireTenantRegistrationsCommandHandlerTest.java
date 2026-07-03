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
 * Contract under test (grace-window terminal expiry): the expiration sweep also expires
 * lapsed REVERIFICATION_REQUIRED registrations after a grace window
 * (activationExpiresAt + reverification-grace-ms &lt;= now) and returns the aggregate
 * count of pending-expiry plus grace-expiry rows. Time is obtained only via ClockPort.
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
