package com.polarishb.pabal.tenant.infrastructure.persistence;

import com.polarishb.pabal.tenant.application.port.out.persistence.TenantRepository;
import com.polarishb.pabal.tenant.contract.persistence.PersistedTenant;
import com.polarishb.pabal.tenant.contract.persistence.TenantPersistenceMapper;
import com.polarishb.pabal.tenant.domain.model.type.TenantStatus;
import com.polarishb.pabal.tenant.infrastructure.persistence.jpa.TenantJpaRepository;
import com.polarishb.pabal.tenant.infrastructure.persistence.jpa.entity.TenantEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class TenantRepositoryImpl implements TenantRepository {

    private final TenantJpaRepository jpaRepository;

    @Override
    public PersistedTenant append(PersistedTenant tenant) {
        TenantEntity saved = jpaRepository.save(TenantEntity.fromNewState(tenant.state()));
        return TenantPersistenceMapper.toPersisted(saved.toState());
    }

    @Override
    public Optional<PersistedTenant> findById(UUID tenantId) {
        return jpaRepository.findById(tenantId)
                .map(TenantEntity::toState)
                .map(TenantPersistenceMapper::toPersisted);
    }

    @Override
    public boolean existsActiveById(UUID tenantId) {
        return jpaRepository.existsByIdAndStatus(tenantId, TenantStatus.ACTIVE);
    }
}
