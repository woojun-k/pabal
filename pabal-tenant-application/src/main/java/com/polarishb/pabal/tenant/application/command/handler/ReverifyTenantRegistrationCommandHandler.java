package com.polarishb.pabal.tenant.application.command.handler;

import com.polarishb.pabal.common.cqrs.CommandHandler;
import com.polarishb.pabal.tenant.application.command.input.ReverifyTenantRegistrationCommand;
import com.polarishb.pabal.tenant.application.command.output.ReverifyTenantRegistrationResult;
import com.polarishb.pabal.tenant.application.port.out.dns.DnsTxtLookupPort;
import com.polarishb.pabal.tenant.application.port.out.persistence.TenantRegistrationRepository;
import com.polarishb.pabal.tenant.application.port.out.time.ClockPort;
import com.polarishb.pabal.tenant.contract.persistence.PersistedTenantRegistration;
import com.polarishb.pabal.tenant.domain.exception.TenantDomainVerificationFailedException;
import com.polarishb.pabal.tenant.domain.exception.TenantRegistrationNotFoundException;
import com.polarishb.pabal.tenant.domain.model.TenantRegistration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

/**
 * ADR-0013 follow-up: single-registration explicit reverification (distinct from the
 * scheduled {@link ReverifyLapsedTenantRegistrationsCommandHandler} sweep). Loads the
 * registration through {@code findByIdForUpdate}, validates the registration status via
 * the read-only {@code TenantRegistration.validateReverificationAllowed()} guard BEFORE
 * performing any DNS lookup, then on a matching DNS TXT re-check transitions
 * REVERIFICATION_REQUIRED -> DOMAIN_VERIFIED via
 * {@link TenantRegistration#reverify(Instant, Instant)} using the registration's existing
 * verification token.
 */
@Component
public class ReverifyTenantRegistrationCommandHandler
        implements CommandHandler<ReverifyTenantRegistrationCommand, ReverifyTenantRegistrationResult> {

    private final TenantRegistrationRepository tenantRegistrationRepository;
    private final DnsTxtLookupPort dnsTxtLookupPort;
    private final ClockPort clockPort;
    private final long activationWindowMs;

    public ReverifyTenantRegistrationCommandHandler(
            TenantRegistrationRepository tenantRegistrationRepository,
            DnsTxtLookupPort dnsTxtLookupPort,
            ClockPort clockPort,
            @Value("${pabal.tenant.registration.activation-window-ms:604800000}") long activationWindowMs
    ) {
        this.tenantRegistrationRepository = tenantRegistrationRepository;
        this.dnsTxtLookupPort = dnsTxtLookupPort;
        this.clockPort = clockPort;
        this.activationWindowMs = activationWindowMs;
    }

    @Override
    @Transactional
    public ReverifyTenantRegistrationResult handle(ReverifyTenantRegistrationCommand command) {
        PersistedTenantRegistration persistedRegistration = tenantRegistrationRepository.findByIdForUpdate(command.registrationId())
                .orElseThrow(() -> new TenantRegistrationNotFoundException(command.registrationId()));
        TenantRegistration registration = persistedRegistration.registration();

        registration.validateReverificationAllowed();

        Set<String> txtRecords = dnsTxtLookupPort.lookupTxtRecords(registration.verificationDnsName());
        if (txtRecords.stream().noneMatch(registration::matchesTxtValue)) {
            throw new TenantDomainVerificationFailedException(
                    registration.getDomainName(),
                    registration.verificationDnsName()
            );
        }

        Instant now = clockPort.now();
        TenantRegistration reverifiedRegistration = registration.reverify(now, now.plus(Duration.ofMillis(activationWindowMs)));
        TenantRegistration savedRegistration = tenantRegistrationRepository.update(
                persistedRegistration.withRegistration(reverifiedRegistration)
        ).registration();

        return new ReverifyTenantRegistrationResult(
                savedRegistration.getId(),
                savedRegistration.getTenantName().value(),
                savedRegistration.getDomainName().value(),
                savedRegistration.getStatus().name(),
                savedRegistration.getVerifiedAt(),
                savedRegistration.getActivationExpiresAt()
        );
    }
}
