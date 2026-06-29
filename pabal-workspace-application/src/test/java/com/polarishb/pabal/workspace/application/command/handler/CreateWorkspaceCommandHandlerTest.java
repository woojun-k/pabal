package com.polarishb.pabal.workspace.application.command.handler;

import com.polarishb.pabal.common.contract.TenantContract;
import com.polarishb.pabal.common.contract.UserContract;
import com.polarishb.pabal.common.exception.InvalidInputException;
import com.polarishb.pabal.workspace.application.command.input.CreateWorkspaceCommand;
import com.polarishb.pabal.workspace.application.command.output.CreateWorkspaceResult;
import com.polarishb.pabal.workspace.application.port.out.persistence.WorkspaceMemberRepository;
import com.polarishb.pabal.workspace.application.port.out.persistence.WorkspaceRepository;
import com.polarishb.pabal.workspace.application.port.out.time.ClockPort;
import com.polarishb.pabal.workspace.contract.persistence.*;
import com.polarishb.pabal.workspace.domain.model.Workspace;
import com.polarishb.pabal.workspace.domain.model.WorkspaceMember;
import com.polarishb.pabal.workspace.domain.model.type.WorkspaceMemberStatus;
import com.polarishb.pabal.workspace.domain.model.type.WorkspaceRole;
import com.polarishb.pabal.workspace.domain.model.type.WorkspaceStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CreateWorkspaceCommandHandlerTest {

    private static final Instant NOW = Instant.parse("2026-06-19T00:00:00Z");

    private final WorkspaceRepository workspaceRepository = mock(WorkspaceRepository.class);
    private final WorkspaceMemberRepository workspaceMemberRepository = mock(WorkspaceMemberRepository.class);
    private final TenantContract tenantContract = mock(TenantContract.class);
    private final UserContract userContract = mock(UserContract.class);
    private final ClockPort clockPort = mock(ClockPort.class);

    private CreateWorkspaceCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new CreateWorkspaceCommandHandler(
                workspaceRepository,
                workspaceMemberRepository,
                tenantContract,
                userContract,
                clockPort
        );
    }

    @Test
    void handle_validates_tenant_and_owner_then_creates_workspace_and_owner_member_in_same_tenant() {
        UUID tenantId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        when(tenantContract.existsActiveTenant(tenantId)).thenReturn(true);
        when(userContract.existsUserInTenant(ownerId, tenantId)).thenReturn(true);
        when(clockPort.now()).thenReturn(NOW);
        when(workspaceRepository.append(any(PersistedWorkspace.class)))
                .thenAnswer(invocation -> savedWorkspace(workspaceId, invocation.getArgument(0)));
        when(workspaceMemberRepository.append(any(PersistedWorkspaceMember.class)))
                .thenAnswer(invocation -> savedMember(memberId, invocation.getArgument(0)));

        CreateWorkspaceResult result = handler.handle(
                new CreateWorkspaceCommand(tenantId, ownerId, "Engineering")
        );

        assertThat(result.workspaceId()).isEqualTo(workspaceId);
        assertThat(result.tenantId()).isEqualTo(tenantId);
        assertThat(result.ownerId()).isEqualTo(ownerId);
        assertThat(result.name()).isEqualTo("Engineering");
        assertThat(result.status()).isEqualTo("ACTIVE");
        assertThat(result.createdAt()).isEqualTo(NOW);

        verify(tenantContract).existsActiveTenant(tenantId);
        verify(userContract).existsUserInTenant(ownerId, tenantId);

        ArgumentCaptor<PersistedWorkspace> workspaceCaptor =
                ArgumentCaptor.forClass(PersistedWorkspace.class);
        verify(workspaceRepository).append(workspaceCaptor.capture());
        assertThat(workspaceCaptor.getValue().workspace().getTenantId()).isEqualTo(tenantId);
        assertThat(workspaceCaptor.getValue().workspace().getCreatedBy()).isEqualTo(ownerId);
        assertThat(workspaceCaptor.getValue().state().version()).isNull();

        ArgumentCaptor<PersistedWorkspaceMember> memberCaptor =
                ArgumentCaptor.forClass(PersistedWorkspaceMember.class);
        verify(workspaceMemberRepository).append(memberCaptor.capture());
        assertThat(memberCaptor.getValue().member().getTenantId()).isEqualTo(tenantId);
        assertThat(memberCaptor.getValue().member().getWorkspaceId()).isEqualTo(workspaceId);
        assertThat(memberCaptor.getValue().member().getUserId()).isEqualTo(ownerId);
        assertThat(memberCaptor.getValue().member().getRole()).isEqualTo(WorkspaceRole.OWNER);
    }

    @Test
    void handle_rejects_inactive_tenant_before_owner_lookup_or_persistence() {
        UUID tenantId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        when(tenantContract.existsActiveTenant(tenantId)).thenReturn(false);

        assertThatThrownBy(() -> handler.handle(
                new CreateWorkspaceCommand(tenantId, ownerId, "Engineering")
        )).isInstanceOf(InvalidInputException.class);

        verify(tenantContract).existsActiveTenant(tenantId);
        verifyNoInteractions(userContract, clockPort, workspaceRepository, workspaceMemberRepository);
    }

    @Test
    void handle_rejects_owner_from_other_tenant_without_persistence() {
        UUID tenantId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        when(tenantContract.existsActiveTenant(tenantId)).thenReturn(true);
        when(userContract.existsUserInTenant(ownerId, tenantId)).thenReturn(false);

        assertThatThrownBy(() -> handler.handle(
                new CreateWorkspaceCommand(tenantId, ownerId, "Engineering")
        )).isInstanceOf(InvalidInputException.class);

        verify(userContract).existsUserInTenant(ownerId, tenantId);
        verifyNoInteractions(clockPort, workspaceRepository, workspaceMemberRepository);
    }

    private PersistedWorkspace savedWorkspace(UUID workspaceId, PersistedWorkspace candidate) {
        Workspace workspace = candidate.workspace();
        WorkspaceState state = new WorkspaceState(
                workspaceId,
                workspace.getTenantId(),
                workspace.getName().value(),
                WorkspaceStatus.ACTIVE,
                workspace.getCreatedBy(),
                workspace.getCreatedAt(),
                workspace.getUpdatedAt(),
                0L
        );
        return WorkspacePersistenceMapper.toPersisted(state);
    }

    private PersistedWorkspaceMember savedMember(UUID memberId, PersistedWorkspaceMember candidate) {
        WorkspaceMember member = candidate.member();
        WorkspaceMemberState state = new WorkspaceMemberState(
                memberId,
                member.getTenantId(),
                member.getWorkspaceId(),
                member.getUserId(),
                member.getRole(),
                WorkspaceMemberStatus.ACTIVE,
                member.getJoinedAt(),
                member.getLeftAt(),
                member.getCreatedAt(),
                member.getUpdatedAt(),
                0L
        );
        return WorkspaceMemberPersistenceMapper.toPersisted(state);
    }
}
