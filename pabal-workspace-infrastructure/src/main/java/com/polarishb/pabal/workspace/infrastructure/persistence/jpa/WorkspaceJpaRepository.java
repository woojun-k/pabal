package com.polarishb.pabal.workspace.infrastructure.persistence.jpa;

import com.polarishb.pabal.workspace.domain.model.type.WorkspaceStatus;
import com.polarishb.pabal.workspace.infrastructure.persistence.jpa.entity.WorkspaceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface WorkspaceJpaRepository extends JpaRepository<WorkspaceEntity, UUID> {
    Optional<WorkspaceEntity> findByTenantIdAndId(UUID tenantId, UUID id);
    boolean existsByTenantIdAndIdAndStatus(UUID tenantId, UUID id, WorkspaceStatus status);
}
