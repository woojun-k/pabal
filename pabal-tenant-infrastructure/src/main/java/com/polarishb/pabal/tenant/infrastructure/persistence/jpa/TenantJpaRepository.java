package com.polarishb.pabal.tenant.infrastructure.persistence.jpa;

import com.polarishb.pabal.tenant.domain.model.type.TenantStatus;
import com.polarishb.pabal.tenant.infrastructure.persistence.jpa.entity.TenantEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TenantJpaRepository extends JpaRepository<TenantEntity, UUID> {
    boolean existsByIdAndStatus(UUID id, TenantStatus status);
}
