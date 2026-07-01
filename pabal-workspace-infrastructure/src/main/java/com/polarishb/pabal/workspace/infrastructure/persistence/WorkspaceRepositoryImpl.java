package com.polarishb.pabal.workspace.infrastructure.persistence;

import com.polarishb.pabal.workspace.application.port.out.persistence.WorkspaceRepository;
import com.polarishb.pabal.workspace.contract.persistence.PersistedWorkspace;
import com.polarishb.pabal.workspace.contract.persistence.WorkspacePersistenceMapper;
import com.polarishb.pabal.workspace.contract.persistence.WorkspaceState;
import com.polarishb.pabal.workspace.domain.model.type.WorkspaceStatus;
import com.polarishb.pabal.workspace.infrastructure.persistence.jpa.WorkspaceJpaRepository;
import com.polarishb.pabal.workspace.infrastructure.persistence.jpa.entity.WorkspaceEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class WorkspaceRepositoryImpl implements WorkspaceRepository {

    private final WorkspaceJpaRepository jpaRepository;

    @Override
    public PersistedWorkspace append(PersistedWorkspace workspace) {
        WorkspaceEntity saved = jpaRepository.save(WorkspaceEntity.fromNewState(workspace.state()));
        return WorkspacePersistenceMapper.toPersisted(saved.toState());
    }

    @Override
    public Optional<PersistedWorkspace> findByTenantIdAndId(UUID tenantId, UUID workspaceId) {
        return jpaRepository.findByTenantIdAndId(tenantId, workspaceId)
                .map(WorkspaceEntity::toState)
                .map(WorkspacePersistenceMapper::toPersisted);
    }

    @Override
    public Optional<WorkspaceState> findStateByTenantIdAndId(UUID tenantId, UUID workspaceId) {
        return jpaRepository.findByTenantIdAndId(tenantId, workspaceId)
                .map(WorkspaceEntity::toState);
    }

    @Override
    public boolean existsActiveByTenantIdAndId(UUID tenantId, UUID workspaceId) {
        return jpaRepository.existsByTenantIdAndIdAndStatus(tenantId, workspaceId, WorkspaceStatus.ACTIVE);
    }
}
