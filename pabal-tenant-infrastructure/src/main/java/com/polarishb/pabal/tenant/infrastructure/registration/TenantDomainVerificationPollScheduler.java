package com.polarishb.pabal.tenant.infrastructure.registration;

import com.polarishb.pabal.tenant.application.command.handler.PollTenantDomainVerificationsCommandHandler;
import com.polarishb.pabal.tenant.application.command.input.PollTenantDomainVerificationsCommand;
import com.polarishb.pabal.tenant.application.command.output.PollTenantDomainVerificationsResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TenantDomainVerificationPollScheduler {

    private final PollTenantDomainVerificationsCommandHandler handler;
    private final int batchSize;

    public TenantDomainVerificationPollScheduler(
            PollTenantDomainVerificationsCommandHandler handler,
            @Value("${pabal.tenant.registration.verification-poll-batch-size:100}") int batchSize
    ) {
        this.handler = handler;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${pabal.tenant.registration.verification-poll-delay-ms:600000}")
    public void pollPendingVerifications() {
        PollTenantDomainVerificationsResult result = handler.handle(
                new PollTenantDomainVerificationsCommand(batchSize)
        );
        if (result.queuedCount() > 0) {
            log.info(
                    "Polled {} tenant domain verification(s): verified={}, failed={}, expired={}, skipped={}",
                    result.queuedCount(),
                    result.verifiedCount(),
                    result.failedCount(),
                    result.expiredCount(),
                    result.skippedCount()
            );
        }
    }
}
