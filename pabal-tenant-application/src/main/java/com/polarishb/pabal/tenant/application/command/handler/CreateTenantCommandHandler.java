package com.polarishb.pabal.tenant.application.command.handler;

import com.polarishb.pabal.common.cqrs.CommandHandler;
import com.polarishb.pabal.tenant.application.command.input.CreateTenantCommand;
import com.polarishb.pabal.tenant.application.command.output.CreateTenantResult;
import com.polarishb.pabal.tenant.application.port.out.persistence.TenantRepository;
import com.polarishb.pabal.tenant.application.port.out.time.ClockPort;
import com.polarishb.pabal.tenant.contract.persistence.PersistedTenant;
import com.polarishb.pabal.tenant.contract.persistence.TenantPersistenceMapper;
import com.polarishb.pabal.tenant.domain.model.Tenant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class CreateTenantCommandHandler implements CommandHandler<CreateTenantCommand, CreateTenantResult> {

    private final TenantRepository tenantRepository;
    private final ClockPort clockPort;

    @Override
    @Transactional
    public CreateTenantResult handle(CreateTenantCommand command) {
        Tenant tenant = Tenant.create(command.name(), clockPort.now());
        PersistedTenant saved = tenantRepository.append(
                new PersistedTenant(tenant, TenantPersistenceMapper.toState(tenant, null))
        );
        return new CreateTenantResult(
                saved.state().id(),
                saved.state().name(),
                saved.state().status().name(),
                saved.state().createdAt()
        );
    }
}
