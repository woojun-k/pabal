package com.polarishb.pabal.tenant.infrastructure.registration;

import com.polarishb.pabal.tenant.application.command.handler.ExpireTenantRegistrationsCommandHandler;
import com.polarishb.pabal.tenant.application.command.handler.ReverifyLapsedTenantRegistrationsCommandHandler;
import com.polarishb.pabal.tenant.application.command.input.ExpireTenantRegistrationsCommand;
import com.polarishb.pabal.tenant.application.command.input.ReverifyLapsedTenantRegistrationsCommand;
import com.polarishb.pabal.tenant.application.port.out.persistence.TenantRegistrationRepository;
import com.polarishb.pabal.tenant.application.port.out.time.ClockPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Component
public class TenantRegistrationExpirationScheduler {

    private final ExpireTenantRegistrationsCommandHandler handler;
    private final ReverifyLapsedTenantRegistrationsCommandHandler reverifyHandler;
    private final TenantRegistrationRepository tenantRegistrationRepository;
    private final ClockPort clockPort;
    private final long reverificationGraceMs;

    public TenantRegistrationExpirationScheduler(
            ExpireTenantRegistrationsCommandHandler handler,
            ReverifyLapsedTenantRegistrationsCommandHandler reverifyHandler,
            TenantRegistrationRepository tenantRegistrationRepository,
            ClockPort clockPort,
            @Value("${pabal.tenant.registration.reverification-grace-ms:604800000}") long reverificationGraceMs
    ) {
        this.handler = handler;
        this.reverifyHandler = reverifyHandler;
        this.tenantRegistrationRepository = tenantRegistrationRepository;
        this.clockPort = clockPort;
        this.reverificationGraceMs = reverificationGraceMs;
    }

    /**
     * ADR-0013 follow-up, grace-window terminal expiry: in addition to the pre-existing
     * PENDING_VERIFICATION expiry sweep, also bulk-expires REVERIFICATION_REQUIRED rows
     * whose {@code activationExpiresAt + reverification-grace-ms <= now} via
     * {@link TenantRegistrationRepository#expireLapsedReverificationRegistrations(Instant, long)}.
     * DOMAIN_VERIFIED rows are untouched here - they must lapse to REVERIFICATION_REQUIRED
     * through {@link #reverifyLapsedRegistrations()} first.
     */
    @Scheduled(fixedDelayString = "${pabal.tenant.registration.expiration-sweep-delay-ms:600000}")
    @Transactional
    public void expirePendingRegistrations() {
        int expiredCount = handler.handle(new ExpireTenantRegistrationsCommand());
        if (expiredCount > 0) {
            log.info("Expired {} pending tenant registration(s)", expiredCount);
        }

        int graceExpiredCount = tenantRegistrationRepository.expireLapsedReverificationRegistrations(
                clockPort.now(),
                reverificationGraceMs
        );
        if (graceExpiredCount > 0) {
            log.info("Terminally expired {} lapsed reverification-required tenant registration(s)", graceExpiredCount);
        }
    }

    @Scheduled(fixedDelayString = "${pabal.tenant.registration.reverification-sweep-delay-ms:600000}")
    public void reverifyLapsedRegistrations() {
        int reverifiedCount = reverifyHandler.handle(new ReverifyLapsedTenantRegistrationsCommand());
        if (reverifiedCount > 0) {
            log.info("Transitioned {} lapsed tenant registration(s) to REVERIFICATION_REQUIRED", reverifiedCount);
        }
    }
}
