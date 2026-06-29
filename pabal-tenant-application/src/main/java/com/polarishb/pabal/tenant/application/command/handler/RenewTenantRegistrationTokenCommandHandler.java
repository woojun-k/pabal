package com.polarishb.pabal.tenant.application.command.handler;

import com.polarishb.pabal.common.cqrs.CommandHandler;
import com.polarishb.pabal.tenant.application.command.input.RenewTenantRegistrationTokenCommand;
import com.polarishb.pabal.tenant.application.command.output.TenantRegistrationResult;
import com.polarishb.pabal.tenant.application.port.out.persistence.TenantRegistrationRepository;
import com.polarishb.pabal.tenant.application.port.out.time.ClockPort;
import com.polarishb.pabal.tenant.application.port.out.token.TenantVerificationTokenGeneratorPort;
import com.polarishb.pabal.tenant.contract.persistence.PersistedTenantRegistration;
import com.polarishb.pabal.tenant.domain.exception.TenantRegistrationExpiredException;
import com.polarishb.pabal.tenant.domain.exception.TenantRegistrationNotFoundException;
import com.polarishb.pabal.tenant.domain.model.TenantRegistration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class RenewTenantRegistrationTokenCommandHandler implements CommandHandler<RenewTenantRegistrationTokenCommand, TenantRegistrationResult> {

    private static final Duration VERIFICATION_TTL = Duration.ofDays(7);

    private final TenantRegistrationRepository tenantRegistrationRepository;
    private final TenantVerificationTokenGeneratorPort tokenGeneratorPort;
    private final ClockPort clockPort;

    @Override
    @Transactional(noRollbackFor = TenantRegistrationExpiredException.class)
    public TenantRegistrationResult handle(RenewTenantRegistrationTokenCommand command) {
        Instant now = clockPort.now();
        PersistedTenantRegistration persistedRegistration = tenantRegistrationRepository.findByIdForUpdate(command.registrationId())
                .orElseThrow(() -> new TenantRegistrationNotFoundException(command.registrationId()));

        TenantRegistration registration = persistedRegistration.registration();
        if (!now.isBefore(registration.getExpiresAt())) {
            Instant expiredAt = registration.getExpiresAt();
            TenantRegistration expiredRegistration = registration.expire(now);
            tenantRegistrationRepository.update(
                    persistedRegistration.withRegistration(expiredRegistration)
            );
            throw new TenantRegistrationExpiredException(command.registrationId(), expiredAt);
        }

        TenantRegistration renewedRegistration = registration.renewVerificationToken(
                tokenGeneratorPort.generate(),
                now,
                now.plus(VERIFICATION_TTL)
        );

        TenantRegistration saved = tenantRegistrationRepository.update(
                persistedRegistration.withRegistration(renewedRegistration)
        ).registration();

        return new TenantRegistrationResult(
                saved.getId(),
                saved.getTenantName().value(),
                saved.getDomainName().value(),
                saved.getStatus().name(),
                saved.verificationDnsName(),
                saved.verificationTxtValue(),
                saved.getExpiresAt(),
                saved.getCreatedAt()
        );
    }
}
