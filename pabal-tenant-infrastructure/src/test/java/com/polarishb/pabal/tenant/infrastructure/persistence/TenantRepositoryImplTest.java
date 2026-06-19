package com.polarishb.pabal.tenant.infrastructure.persistence;

import com.polarishb.pabal.support.AbstractTenantPostgresDataJpaTest;
import com.polarishb.pabal.tenant.application.port.out.persistence.TenantRepository;
import com.polarishb.pabal.tenant.contract.persistence.PersistedTenant;
import com.polarishb.pabal.tenant.contract.persistence.TenantPersistenceMapper;
import com.polarishb.pabal.tenant.domain.model.Tenant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class TenantRepositoryImplTest extends AbstractTenantPostgresDataJpaTest {

    @Autowired
    private TenantRepository tenantRepository;

    @Test
    void append_and_findById_round_trips_active_tenant() {
        Instant now = Instant.parse("2026-04-08T00:00:00Z");
        Tenant tenant = Tenant.create("Acme", now);

        PersistedTenant saved = tenantRepository.append(
                new PersistedTenant(tenant, TenantPersistenceMapper.toState(tenant, null))
        );
        Optional<PersistedTenant> found = tenantRepository.findById(saved.state().id());

        assertThat(saved.state().id()).isNotNull();
        assertThat(saved.state().name()).isEqualTo("Acme");
        assertThat(saved.state().version()).isEqualTo(0L);
        assertThat(found).isPresent();
        assertThat(found.orElseThrow().state().id()).isEqualTo(saved.state().id());
        assertThat(tenantRepository.existsActiveById(saved.state().id())).isTrue();
    }
}
