package com.polarishb.pabal.tenant.application.command.handler;

import com.polarishb.pabal.common.cqrs.CommandHandler;
import com.polarishb.pabal.tenant.application.command.input.ReverifyLapsedTenantRegistrationsCommand;
import com.polarishb.pabal.tenant.application.port.out.persistence.TenantRegistrationRepository;
import com.polarishb.pabal.tenant.application.port.out.time.ClockPort;
import com.polarishb.pabal.tenant.contract.persistence.PersistedTenantRegistration;
import com.polarishb.pabal.tenant.domain.exception.TenantRegistrationNotPendingException;
import com.polarishb.pabal.tenant.domain.model.TenantRegistration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * ADR-0013 follow-up: sweeps DOMAIN_VERIFIED tenant registrations whose activation
 * window has lapsed and transitions each one to REVERIFICATION_REQUIRED strictly through
 * the domain method {@link TenantRegistration#requireReverification(Instant)} - never via
 * a bulk status UPDATE that bypasses domain invariants.
 *
 * <p>Per-row resilient: a candidate id fetched by {@code findLapsedDomainVerifiedIds} may
 * have concurrently changed status or activation window by the time it's locked and
 * reconstituted (findByIdForUpdate). In that case {@code requireReverification} throws
 * either {@link TenantRegistrationNotPendingException} (status no longer DOMAIN_VERIFIED)
 * or {@link IllegalStateException} (activation window no longer lapsed); both are caught
 * per-row so the row is skipped, not counted, and the sweep continues.
 */
@Component
@RequiredArgsConstructor
public class ReverifyLapsedTenantRegistrationsCommandHandler
        implements CommandHandler<ReverifyLapsedTenantRegistrationsCommand, Integer> {

    private static final int FETCH_LIMIT = 100;

    private final TenantRegistrationRepository tenantRegistrationRepository;
    private final ClockPort clockPort;

    @Override
    @Transactional
    public Integer handle(ReverifyLapsedTenantRegistrationsCommand command) {
        Instant now = clockPort.now();
        List<UUID> lapsedIds = tenantRegistrationRepository.findLapsedDomainVerifiedIds(now, FETCH_LIMIT);

        int transitionedCount = 0;
        for (UUID registrationId : lapsedIds) {
            if (reverifyOne(registrationId, now)) {
                transitionedCount++;
            }
        }
        return transitionedCount;
    }

    private boolean reverifyOne(UUID registrationId, Instant now) {
        try {
            PersistedTenantRegistration persisted = tenantRegistrationRepository.findByIdForUpdate(registrationId)
                    .orElse(null);
            if (persisted == null) {
                return false;
            }
            TenantRegistration lapsed = persisted.registration().requireReverification(now);
            tenantRegistrationRepository.update(persisted.withRegistration(lapsed));
            return true;
        } catch (TenantRegistrationNotPendingException | IllegalStateException e) {
            return false;
        }
    }
}
