package com.polarishb.pabal.tenant.application.command.handler;

import com.polarishb.pabal.common.cqrs.CommandHandler;
import com.polarishb.pabal.tenant.application.command.input.ExpireTenantRegistrationsCommand;
import com.polarishb.pabal.tenant.application.port.out.persistence.TenantRegistrationRepository;
import com.polarishb.pabal.tenant.application.port.out.time.ClockPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class ExpireTenantRegistrationsCommandHandler implements CommandHandler<ExpireTenantRegistrationsCommand, Integer> {

    private final TenantRegistrationRepository tenantRegistrationRepository;
    private final ClockPort clockPort;

    @Override
    @Transactional
    public Integer handle(ExpireTenantRegistrationsCommand command) {
        Instant now = clockPort.now();
        return tenantRegistrationRepository.expirePendingRegistrations(now);
    }
}
