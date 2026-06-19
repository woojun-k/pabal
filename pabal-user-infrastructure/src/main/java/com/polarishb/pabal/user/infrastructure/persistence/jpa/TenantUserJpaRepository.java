package com.polarishb.pabal.user.infrastructure.persistence.jpa;

import com.polarishb.pabal.user.domain.model.type.UserStatus;
import com.polarishb.pabal.user.infrastructure.persistence.jpa.entity.TenantUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface TenantUserJpaRepository extends JpaRepository<TenantUserEntity, UUID> {

    Optional<TenantUserEntity> findByTenantIdAndId(UUID tenantId, UUID id);

    boolean existsByTenantIdAndIdAndStatus(UUID tenantId, UUID id, UserStatus status);

    @Query("""
            select tenantUser.id
            from TenantUserEntity tenantUser
            where tenantUser.tenantId = :tenantId
              and tenantUser.id in :ids
              and tenantUser.status = :status
            """)
    Set<UUID> findIdsByTenantIdAndIdInAndStatus(
            @Param("tenantId") UUID tenantId,
            @Param("ids") Set<UUID> ids,
            @Param("status") UserStatus status
    );
}
