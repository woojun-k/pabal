package com.polarishb.pabal.tenant.infrastructure.registration;

import com.polarishb.pabal.tenant.application.command.handler.ExpireTenantRegistrationsCommandHandler;
import com.polarishb.pabal.tenant.application.command.input.ExpireTenantRegistrationsCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TenantRegistrationExpirationScheduler {

    private final ExpireTenantRegistrationsCommandHandler handler;

    @Scheduled(fixedDelayString = "${pabal.tenant.registration.expiration-sweep-delay-ms:600000}")
    public void expirePendingRegistrations() {
        int expiredCount = handler.handle(new ExpireTenantRegistrationsCommand());
        if (expiredCount > 0) {
            log.info("Expired {} pending tenant registration(s)", expiredCount);
        }
    }
}
