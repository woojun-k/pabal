package com.polarishb.pabal.tenant.application.command.handler;

import com.polarishb.pabal.common.cqrs.CommandHandler;
import com.polarishb.pabal.tenant.application.command.input.PollTenantDomainVerificationsCommand;
import com.polarishb.pabal.tenant.application.command.input.VerifyTenantDomainCommand;
import com.polarishb.pabal.tenant.application.command.output.PollTenantDomainVerificationsResult;
import com.polarishb.pabal.tenant.application.port.out.persistence.TenantRegistrationRepository;
import com.polarishb.pabal.tenant.application.port.out.time.ClockPort;
import com.polarishb.pabal.tenant.domain.exception.TenantDomainVerificationFailedException;
import com.polarishb.pabal.tenant.domain.exception.TenantRegistrationExpiredException;
import com.polarishb.pabal.tenant.domain.exception.TenantRegistrationNotFoundException;
import com.polarishb.pabal.tenant.domain.exception.TenantRegistrationNotPendingException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PollTenantDomainVerificationsCommandHandler implements CommandHandler<PollTenantDomainVerificationsCommand, PollTenantDomainVerificationsResult> {

    private final TenantRegistrationRepository tenantRegistrationRepository;
    private final VerifyTenantDomainCommandHandler verifyTenantDomainCommandHandler;
    private final ClockPort clockPort;

    @Override
    public PollTenantDomainVerificationsResult handle(PollTenantDomainVerificationsCommand command) {
        Instant now = clockPort.now();
        List<UUID> registrationIds = tenantRegistrationRepository.findPendingVerificationIds(now, command.limit());

        int verifiedCount = 0;
        int failedCount = 0;
        int expiredCount = 0;
        int skippedCount = 0;

        for (UUID registrationId : registrationIds) {
            try {
                verifyTenantDomainCommandHandler.handle(new VerifyTenantDomainCommand(registrationId));
                verifiedCount++;
            } catch (TenantDomainVerificationFailedException e) {
                failedCount++;
            } catch (TenantRegistrationExpiredException e) {
                expiredCount++;
            } catch (TenantRegistrationNotFoundException | TenantRegistrationNotPendingException e) {
                skippedCount++;
            }
        }

        return new PollTenantDomainVerificationsResult(
                registrationIds.size(),
                verifiedCount,
                failedCount,
                expiredCount,
                skippedCount
        );
    }
}
