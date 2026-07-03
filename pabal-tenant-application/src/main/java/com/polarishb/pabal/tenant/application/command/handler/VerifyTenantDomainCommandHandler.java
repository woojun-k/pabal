package com.polarishb.pabal.tenant.application.command.handler;

import com.polarishb.pabal.common.cqrs.CommandHandler;
import com.polarishb.pabal.tenant.application.command.input.VerifyTenantDomainCommand;
import com.polarishb.pabal.tenant.application.command.output.VerifyTenantDomainResult;
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
 * Verify-only phase of the two-phase verify/activate split. Performs
 * the DNS TXT lookup and transitions PENDING_VERIFICATION -> DOMAIN_VERIFIED via
 * {@link TenantRegistration#markVerified(Instant, Instant)}, persisting exactly one
 * registration update. It stops there - no {@code Tenant} is created and this handler has
 * no {@code TenantRepository} dependency. Activation is a separate, explicit step handled
 * by {@link ActivateTenantRegistrationCommandHandler}.
 */
@Component
public class VerifyTenantDomainCommandHandler implements CommandHandler<VerifyTenantDomainCommand, VerifyTenantDomainResult> {

    private final TenantRegistrationRepository tenantRegistrationRepository;
    private final DnsTxtLookupPort dnsTxtLookupPort;
    private final ClockPort clockPort;
    private final long activationWindowMs;

    public VerifyTenantDomainCommandHandler(
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
    public VerifyTenantDomainResult handle(VerifyTenantDomainCommand command) {
        PersistedTenantRegistration persistedRegistration = tenantRegistrationRepository.findByIdForUpdate(command.registrationId())
                .orElseThrow(() -> new TenantRegistrationNotFoundException(command.registrationId()));
        TenantRegistration registration = persistedRegistration.registration();

        Instant now = clockPort.now();
        registration.validateVerificationAllowed(now);

        Set<String> txtRecords = dnsTxtLookupPort.lookupTxtRecords(registration.verificationDnsName());
        if (txtRecords.stream().noneMatch(registration::matchesTxtValue)) {
            throw new TenantDomainVerificationFailedException(
                    registration.getDomainName(),
                    registration.verificationDnsName()
            );
        }

        TenantRegistration verifiedRegistration = registration.markVerified(now, now.plus(Duration.ofMillis(activationWindowMs)));
        TenantRegistration savedRegistration = tenantRegistrationRepository.update(
                persistedRegistration.withRegistration(verifiedRegistration)
        ).registration();

        return new VerifyTenantDomainResult(
                savedRegistration.getId(),
                savedRegistration.getTenantName().value(),
                savedRegistration.getDomainName().value(),
                savedRegistration.getStatus().name(),
                savedRegistration.getVerifiedAt(),
                savedRegistration.getActivationExpiresAt()
        );
    }
}
