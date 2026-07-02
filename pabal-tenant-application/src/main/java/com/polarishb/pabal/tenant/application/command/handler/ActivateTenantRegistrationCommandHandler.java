package com.polarishb.pabal.tenant.application.command.handler;

import com.polarishb.pabal.common.cqrs.CommandHandler;
import com.polarishb.pabal.tenant.application.command.input.ActivateTenantRegistrationCommand;
import com.polarishb.pabal.tenant.application.command.output.ActivateTenantRegistrationResult;
import com.polarishb.pabal.tenant.application.port.out.persistence.TenantRegistrationRepository;
import com.polarishb.pabal.tenant.application.port.out.persistence.TenantRepository;
import com.polarishb.pabal.tenant.application.port.out.time.ClockPort;
import com.polarishb.pabal.tenant.contract.persistence.PersistedTenant;
import com.polarishb.pabal.tenant.contract.persistence.PersistedTenantRegistration;
import com.polarishb.pabal.tenant.contract.persistence.TenantPersistenceMapper;
import com.polarishb.pabal.tenant.domain.exception.TenantRegistrationExpiredException;
import com.polarishb.pabal.tenant.domain.exception.TenantRegistrationNotFoundException;
import com.polarishb.pabal.tenant.domain.exception.TenantRegistrationNotPendingException;
import com.polarishb.pabal.tenant.domain.model.Tenant;
import com.polarishb.pabal.tenant.domain.model.TenantRegistration;
import com.polarishb.pabal.tenant.domain.model.type.TenantRegistrationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * ADR-0013 follow-up: activation phase of the two-phase verify/activate split. Locks the
 * registration via {@code findByIdForUpdate}, transitions DOMAIN_VERIFIED (window open)
 * -> ACTIVATED via {@link TenantRegistration#activate(UUID, Instant)}, creates exactly
 * one {@code Tenant}, and links its id to the registration.
 *
 * <p>No {@code Tenant} row may be persisted for a failed activation. Because
 * {@code activate} needs a {@code tenantId} argument before it can validate the
 * transition, the handler mirrors {@code activate}'s own status/window preconditions
 * read-only (via existing {@code getStatus()}/{@code getActivationExpiresAt()} accessors -
 * the same read-then-decide pattern {@code RenewTenantRegistrationTokenCommandHandler}
 * already uses) to fail fast before touching {@link TenantRepository}. The actual state
 * transition is still performed exclusively by {@code activate}, which re-validates the
 * same invariants, so no transition ever bypasses the domain method.
 */
@Component
@RequiredArgsConstructor
public class ActivateTenantRegistrationCommandHandler
        implements CommandHandler<ActivateTenantRegistrationCommand, ActivateTenantRegistrationResult> {

    private final TenantRegistrationRepository tenantRegistrationRepository;
    private final TenantRepository tenantRepository;
    private final ClockPort clockPort;

    @Override
    @Transactional
    public ActivateTenantRegistrationResult handle(ActivateTenantRegistrationCommand command) {
        PersistedTenantRegistration persistedRegistration = tenantRegistrationRepository.findByIdForUpdate(command.registrationId())
                .orElseThrow(() -> new TenantRegistrationNotFoundException(command.registrationId()));
        TenantRegistration registration = persistedRegistration.registration();

        Instant now = clockPort.now();
        if (registration.getStatus() != TenantRegistrationStatus.DOMAIN_VERIFIED) {
            throw new TenantRegistrationNotPendingException(registration.getId(), registration.getStatus());
        }
        if (!now.isBefore(registration.getActivationExpiresAt())) {
            throw new TenantRegistrationExpiredException(registration.getId(), registration.getActivationExpiresAt());
        }

        Tenant tenant = Tenant.create(registration.getTenantName().value(), now);
        PersistedTenant savedTenant = tenantRepository.append(
                new PersistedTenant(tenant, TenantPersistenceMapper.toState(tenant, null))
        );

        TenantRegistration activatedRegistration = registration.activate(savedTenant.state().id(), now);
        TenantRegistration savedRegistration = tenantRegistrationRepository.update(
                persistedRegistration.withRegistration(activatedRegistration)
        ).registration();

        return new ActivateTenantRegistrationResult(
                savedRegistration.getId(),
                savedRegistration.getTenantId(),
                savedRegistration.getTenantName().value(),
                savedRegistration.getDomainName().value(),
                savedRegistration.getStatus().name(),
                savedRegistration.getVerifiedAt(),
                savedRegistration.getActivatedAt()
        );
    }
}
