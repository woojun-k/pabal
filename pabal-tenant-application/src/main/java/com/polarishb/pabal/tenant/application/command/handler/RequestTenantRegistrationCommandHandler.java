package com.polarishb.pabal.tenant.application.command.handler;

import com.polarishb.pabal.common.cqrs.CommandHandler;
import com.polarishb.pabal.tenant.application.command.input.RequestTenantRegistrationCommand;
import com.polarishb.pabal.tenant.application.command.output.TenantRegistrationResult;
import com.polarishb.pabal.tenant.application.port.out.persistence.TenantRegistrationRepository;
import com.polarishb.pabal.tenant.application.port.out.time.ClockPort;
import com.polarishb.pabal.tenant.application.port.out.token.TenantVerificationTokenGeneratorPort;
import com.polarishb.pabal.tenant.contract.persistence.PersistedTenantRegistration;
import com.polarishb.pabal.tenant.contract.persistence.TenantRegistrationPersistenceMapper;
import com.polarishb.pabal.tenant.domain.exception.TenantDomainAlreadyRegisteredException;
import com.polarishb.pabal.tenant.domain.model.TenantRegistration;
import com.polarishb.pabal.tenant.domain.model.vo.TenantDomainName;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Component
public class RequestTenantRegistrationCommandHandler implements CommandHandler<RequestTenantRegistrationCommand, TenantRegistrationResult> {

    private final TenantRegistrationRepository tenantRegistrationRepository;
    private final TenantVerificationTokenGeneratorPort tokenGeneratorPort;
    private final ClockPort clockPort;
    private final long verificationWindowMs;

    public RequestTenantRegistrationCommandHandler(
            TenantRegistrationRepository tenantRegistrationRepository,
            TenantVerificationTokenGeneratorPort tokenGeneratorPort,
            ClockPort clockPort,
            @Value("${pabal.tenant.registration.verification-window-ms:604800000}") long verificationWindowMs
    ) {
        this.tenantRegistrationRepository = tenantRegistrationRepository;
        this.tokenGeneratorPort = tokenGeneratorPort;
        this.clockPort = clockPort;
        this.verificationWindowMs = verificationWindowMs;
    }

    @Override
    @Transactional
    public TenantRegistrationResult handle(RequestTenantRegistrationCommand command) {
        Instant now = clockPort.now();
        TenantDomainName domainName = new TenantDomainName(command.domainName());
        tenantRegistrationRepository.expirePendingRegistrations(now);
        if (tenantRegistrationRepository.existsOpenByDomainName(domainName)) {
            throw new TenantDomainAlreadyRegisteredException(domainName);
        }

        TenantRegistration registration = TenantRegistration.request(
                command.tenantName(),
                domainName.value(),
                tokenGeneratorPort.generate(),
                now,
                now.plus(Duration.ofMillis(verificationWindowMs))
        );
        PersistedTenantRegistration saved = tenantRegistrationRepository.append(
                new PersistedTenantRegistration(
                        registration,
                        TenantRegistrationPersistenceMapper.toState(registration, null)
                )
        );

        return toResult(saved.registration());
    }

    private TenantRegistrationResult toResult(TenantRegistration registration) {
        return new TenantRegistrationResult(
                registration.getId(),
                registration.getTenantName().value(),
                registration.getDomainName().value(),
                registration.getStatus().name(),
                registration.verificationDnsName(),
                registration.verificationTxtValue(),
                registration.getVerificationExpiresAt(),
                registration.getActivationExpiresAt(),
                registration.getCreatedAt()
        );
    }
}
