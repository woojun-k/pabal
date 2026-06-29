package com.polarishb.pabal.workspace.application.service;

import com.polarishb.pabal.common.contract.dto.WorkspaceMemberRole;
import com.polarishb.pabal.workspace.application.port.out.persistence.WorkspaceMemberRepository;
import com.polarishb.pabal.workspace.domain.model.type.WorkspaceRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class WorkspaceContractServiceTest {

    private final WorkspaceMemberRepository workspaceMemberRepository = mock(WorkspaceMemberRepository.class);

    private WorkspaceContractService service;

    @BeforeEach
    void setUp() {
        service = new WorkspaceContractService(workspaceMemberRepository);
    }

    @Test
    void existsActiveMember_delegates_with_tenant_workspace_and_user_id() {
        UUID tenantId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(workspaceMemberRepository.existsActiveMember(tenantId, workspaceId, userId)).thenReturn(true);

        assertThat(service.existsActiveMember(tenantId, workspaceId, userId)).isTrue();

        verify(workspaceMemberRepository).existsActiveMember(tenantId, workspaceId, userId);
    }

    @Test
    void findActiveMemberIds_delegates_with_tenant_workspace_and_user_ids() {
        UUID tenantId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        UUID activeUserId = UUID.randomUUID();
        Set<UUID> requestedIds = Set.of(activeUserId, UUID.randomUUID());
        when(workspaceMemberRepository.findActiveUserIds(tenantId, workspaceId, requestedIds))
                .thenReturn(Set.of(activeUserId));

        Set<UUID> result = service.findActiveMemberIds(tenantId, workspaceId, requestedIds);

        assertThat(result).containsExactly(activeUserId);
        verify(workspaceMemberRepository).findActiveUserIds(tenantId, workspaceId, requestedIds);
    }

    @Test
    void findActiveMemberRole_delegates_with_tenant_workspace_and_user_id() {
        UUID tenantId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(workspaceMemberRepository.findActiveRole(tenantId, workspaceId, userId))
                .thenReturn(Optional.of(WorkspaceRole.OWNER));

        Optional<WorkspaceMemberRole> result = service.findActiveMemberRole(tenantId, workspaceId, userId);

        assertThat(result).contains(WorkspaceMemberRole.OWNER);
        verify(workspaceMemberRepository).findActiveRole(tenantId, workspaceId, userId);
    }
}
