package com.polarishb.pabal.user.infrastructure.persistence.jpa;

import com.polarishb.pabal.user.domain.model.type.UserStatus;
import com.polarishb.pabal.user.infrastructure.persistence.jpa.entity.TenantUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TenantUserJpaRepository extends JpaRepository<TenantUserEntity, UUID> {

    Optional<TenantUserEntity> findByTenantIdAndId(UUID tenantId, UUID id);

    boolean existsByTenantIdAndIdAndStatus(UUID tenantId, UUID id, UserStatus status);
}
