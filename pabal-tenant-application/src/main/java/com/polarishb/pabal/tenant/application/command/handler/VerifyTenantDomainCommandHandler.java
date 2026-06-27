package com.polarishb.pabal.tenant.application.command.handler;

import com.polarishb.pabal.common.cqrs.CommandHandler;
import com.polarishb.pabal.tenant.application.command.input.VerifyTenantDomainCommand;
import com.polarishb.pabal.tenant.application.command.output.VerifyTenantDomainResult;
import com.polarishb.pabal.tenant.application.port.out.dns.DnsTxtLookupPort;
import com.polarishb.pabal.tenant.application.port.out.persistence.TenantRegistrationRepository;
import com.polarishb.pabal.tenant.application.port.out.persistence.TenantRepository;
import com.polarishb.pabal.tenant.application.port.out.time.ClockPort;
import com.polarishb.pabal.tenant.contract.persistence.PersistedTenant;
import com.polarishb.pabal.tenant.contract.persistence.PersistedTenantRegistration;
import com.polarishb.pabal.tenant.contract.persistence.TenantPersistenceMapper;
import com.polarishb.pabal.tenant.contract.persistence.TenantRegistrationPersistenceMapper;
import com.polarishb.pabal.tenant.domain.exception.TenantDomainVerificationFailedException;
import com.polarishb.pabal.tenant.domain.exception.TenantRegistrationNotFoundException;
import com.polarishb.pabal.tenant.domain.model.Tenant;
import com.polarishb.pabal.tenant.domain.model.TenantRegistration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class VerifyTenantDomainCommandHandler implements CommandHandler<VerifyTenantDomainCommand, VerifyTenantDomainResult> {

    private final TenantRegistrationRepository tenantRegistrationRepository;
    private final TenantRepository tenantRepository;
    private final DnsTxtLookupPort dnsTxtLookupPort;
    private final ClockPort clockPort;

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

        registration.markVerified(now);

        Tenant tenant = Tenant.create(registration.getTenantName().value(), now);
        PersistedTenant savedTenant = tenantRepository.append(
                new PersistedTenant(tenant, TenantPersistenceMapper.toState(tenant, null))
        );

        registration.activate(savedTenant.state().id(), now);
        TenantRegistration savedRegistration = tenantRegistrationRepository.update(
                new PersistedTenantRegistration(
                        registration,
                        TenantRegistrationPersistenceMapper.toState(registration, persistedRegistration.state().version())
                )
        ).registration();

        return new VerifyTenantDomainResult(
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
