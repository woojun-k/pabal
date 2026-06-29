package com.polarishb.pabal.workspace.application.port.out.persistence;

import com.polarishb.pabal.workspace.contract.persistence.PersistedWorkspace;

import java.util.Optional;
import java.util.UUID;

public interface WorkspaceRepository {
    PersistedWorkspace append(PersistedWorkspace workspace);
    Optional<PersistedWorkspace> findByTenantIdAndId(UUID tenantId, UUID workspaceId);
    boolean existsActiveByTenantIdAndId(UUID tenantId, UUID workspaceId);
}
