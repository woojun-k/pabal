package com.polarishb.pabal.tenant.application.command.handler;

import com.polarishb.pabal.tenant.application.command.input.CreateTenantCommand;
import com.polarishb.pabal.tenant.application.command.output.CreateTenantResult;
import com.polarishb.pabal.tenant.application.port.out.persistence.TenantRepository;
import com.polarishb.pabal.tenant.application.port.out.time.ClockPort;
import com.polarishb.pabal.tenant.contract.persistence.PersistedTenant;
import com.polarishb.pabal.tenant.contract.persistence.TenantPersistenceMapper;
import com.polarishb.pabal.tenant.contract.persistence.TenantState;
import com.polarishb.pabal.tenant.domain.model.Tenant;
import com.polarishb.pabal.tenant.domain.model.type.TenantStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateTenantCommandHandlerTest {

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private ClockPort clockPort;

    @InjectMocks
    private CreateTenantCommandHandler handler;

    @Test
    void handle_creates_active_tenant_with_application_time() {
        UUID tenantId = UUID.randomUUID();
        Instant now = Instant.parse("2026-04-08T00:00:00Z");
        when(clockPort.now()).thenReturn(now);
        when(tenantRepository.append(any(PersistedTenant.class)))
                .thenAnswer(invocation -> persisted(invocation.getArgument(0), tenantId, 0L));

        CreateTenantResult result = handler.handle(new CreateTenantCommand("Acme"));

        assertThat(result.tenantId()).isEqualTo(tenantId);
        assertThat(result.name()).isEqualTo("Acme");
        assertThat(result.status()).isEqualTo("ACTIVE");
        assertThat(result.createdAt()).isEqualTo(now);
        verify(clockPort).now();
        verify(tenantRepository).append(any(PersistedTenant.class));
    }

    private PersistedTenant persisted(PersistedTenant tenant, UUID tenantId, Long version) {
        TenantState state = new TenantState(
                tenantId,
                tenant.state().name(),
                TenantStatus.ACTIVE,
                tenant.state().createdAt(),
                tenant.state().updatedAt(),
                version
        );
        Tenant domain = TenantPersistenceMapper.toDomain(state);
        return new PersistedTenant(domain, state);
    }
}
