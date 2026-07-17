package com.polarishb.pabal.workspace.application.service;

import com.polarishb.pabal.integration.contract.WorkspaceContract;
import com.polarishb.pabal.integration.contract.dto.WorkspaceMemberRole;
import com.polarishb.pabal.workspace.application.port.out.persistence.WorkspaceMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkspaceContractService implements WorkspaceContract {

    private final WorkspaceMemberRepository workspaceMemberRepository;

    @Override
    @Transactional(readOnly = true)
    public boolean existsActiveMember(UUID tenantId, UUID workspaceId, UUID userId) {
        return workspaceMemberRepository.existsActiveMember(tenantId, workspaceId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<UUID> findActiveMemberIds(UUID tenantId, UUID workspaceId, Set<UUID> userIds) {
        return workspaceMemberRepository.findActiveUserIds(tenantId, workspaceId, userIds);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<WorkspaceMemberRole> findActiveMemberRole(UUID tenantId, UUID workspaceId, UUID userId) {
        return workspaceMemberRepository.findActiveRole(tenantId, workspaceId, userId)
                .map(role -> WorkspaceMemberRole.valueOf(role.name()));
    }
}
