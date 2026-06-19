package com.polarishb.pabal.workspace.application.command.handler;

import com.polarishb.pabal.common.contract.TenantContract;
import com.polarishb.pabal.common.contract.UserContract;
import com.polarishb.pabal.common.exception.InvalidInputException;
import com.polarishb.pabal.workspace.application.command.input.CreateWorkspaceCommand;
import com.polarishb.pabal.workspace.application.command.output.CreateWorkspaceResult;
import com.polarishb.pabal.workspace.application.port.out.persistence.WorkspaceMemberRepository;
import com.polarishb.pabal.workspace.application.port.out.persistence.WorkspaceRepository;
import com.polarishb.pabal.workspace.application.port.out.time.ClockPort;
import com.polarishb.pabal.workspace.contract.persistence.PersistedWorkspace;
import com.polarishb.pabal.workspace.contract.persistence.PersistedWorkspaceMember;
import com.polarishb.pabal.workspace.contract.persistence.WorkspaceMemberPersistenceMapper;
import com.polarishb.pabal.workspace.contract.persistence.WorkspaceMemberState;
import com.polarishb.pabal.workspace.contract.persistence.WorkspacePersistenceMapper;
import com.polarishb.pabal.workspace.contract.persistence.WorkspaceState;
import com.polarishb.pabal.workspace.domain.model.Workspace;
import com.polarishb.pabal.workspace.domain.model.WorkspaceMember;
import com.polarishb.pabal.workspace.domain.model.type.WorkspaceMemberStatus;
import com.polarishb.pabal.workspace.domain.model.type.WorkspaceRole;
import com.polarishb.pabal.workspace.domain.model.type.WorkspaceStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateWorkspaceCommandHandlerTest {

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private WorkspaceMemberRepository workspaceMemberRepository;

    @Mock
    private TenantContract tenantContract;

    @Mock
    private UserContract userContract;

    @Mock
    private ClockPort clockPort;

    @InjectMocks
    private CreateWorkspaceCommandHandler handler;

    @Test
    void handle_creates_workspace_and_owner_member() {
        UUID tenantId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        Instant now = Instant.parse("2026-04-08T00:00:00Z");
        when(tenantContract.existsActiveTenant(tenantId)).thenReturn(true);
        when(userContract.existsUserInTenant(ownerId, tenantId)).thenReturn(true);
        when(clockPort.now()).thenReturn(now);
        when(workspaceRepository.append(any(PersistedWorkspace.class)))
                .thenAnswer(invocation -> persistedWorkspace(invocation.getArgument(0), workspaceId, 0L));
        when(workspaceMemberRepository.append(any(PersistedWorkspaceMember.class)))
                .thenAnswer(invocation -> persistedMember(invocation.getArgument(0), memberId, workspaceId, 0L));

        CreateWorkspaceResult result = handler.handle(
                new CreateWorkspaceCommand(tenantId, ownerId, "Engineering")
        );

        assertThat(result.workspaceId()).isEqualTo(workspaceId);
        assertThat(result.tenantId()).isEqualTo(tenantId);
        assertThat(result.name()).isEqualTo("Engineering");
        assertThat(result.status()).isEqualTo("ACTIVE");
        assertThat(result.ownerId()).isEqualTo(ownerId);
        assertThat(result.createdAt()).isEqualTo(now);
        verify(workspaceRepository).append(any(PersistedWorkspace.class));
        verify(workspaceMemberRepository).append(any(PersistedWorkspaceMember.class));
    }

    @Test
    void handle_rejects_inactive_tenant_before_owner_lookup() {
        UUID tenantId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        when(tenantContract.existsActiveTenant(tenantId)).thenReturn(false);

        assertThatThrownBy(() -> handler.handle(new CreateWorkspaceCommand(tenantId, ownerId, "Engineering")))
                .isInstanceOf(InvalidInputException.class);

        verifyNoInteractions(userContract, clockPort, workspaceRepository, workspaceMemberRepository);
    }

    @Test
    void handle_rejects_owner_outside_tenant_before_clock_and_save() {
        UUID tenantId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        when(tenantContract.existsActiveTenant(tenantId)).thenReturn(true);
        when(userContract.existsUserInTenant(ownerId, tenantId)).thenReturn(false);

        assertThatThrownBy(() -> handler.handle(new CreateWorkspaceCommand(tenantId, ownerId, "Engineering")))
                .isInstanceOf(InvalidInputException.class);

        verifyNoInteractions(clockPort, workspaceRepository, workspaceMemberRepository);
    }

    private PersistedWorkspace persistedWorkspace(PersistedWorkspace workspace, UUID workspaceId, Long version) {
        WorkspaceState state = new WorkspaceState(
                workspaceId,
                workspace.state().tenantId(),
                workspace.state().name(),
                WorkspaceStatus.ACTIVE,
                workspace.state().createdBy(),
                workspace.state().createdAt(),
                workspace.state().updatedAt(),
                version
        );
        Workspace domain = WorkspacePersistenceMapper.toDomain(state);
        return new PersistedWorkspace(domain, state);
    }

    private PersistedWorkspaceMember persistedMember(
            PersistedWorkspaceMember member,
            UUID memberId,
            UUID workspaceId,
            Long version
    ) {
        WorkspaceMemberState state = new WorkspaceMemberState(
                memberId,
                member.state().tenantId(),
                workspaceId,
                member.state().userId(),
                WorkspaceRole.OWNER,
                WorkspaceMemberStatus.ACTIVE,
                member.state().joinedAt(),
                null,
                member.state().createdAt(),
                member.state().updatedAt(),
                version
        );
        WorkspaceMember domain = WorkspaceMemberPersistenceMapper.toDomain(state);
        return new PersistedWorkspaceMember(domain, state);
    }
}
