package com.polarishb.pabal.workspace.application.command.handler;

import com.polarishb.pabal.integration.contract.TenantContract;
import com.polarishb.pabal.integration.contract.UserContract;
import com.polarishb.pabal.common.cqrs.CommandHandler;
import com.polarishb.pabal.common.exception.InvalidInputException;
import com.polarishb.pabal.workspace.application.command.input.CreateWorkspaceCommand;
import com.polarishb.pabal.workspace.application.command.output.CreateWorkspaceResult;
import com.polarishb.pabal.workspace.application.port.out.persistence.WorkspaceMemberRepository;
import com.polarishb.pabal.workspace.application.port.out.persistence.WorkspaceRepository;
import com.polarishb.pabal.workspace.application.port.out.time.ClockPort;
import com.polarishb.pabal.workspace.contract.persistence.PersistedWorkspace;
import com.polarishb.pabal.workspace.contract.persistence.PersistedWorkspaceMember;
import com.polarishb.pabal.workspace.contract.persistence.WorkspaceMemberPersistenceMapper;
import com.polarishb.pabal.workspace.contract.persistence.WorkspacePersistenceMapper;
import com.polarishb.pabal.workspace.domain.model.Workspace;
import com.polarishb.pabal.workspace.domain.model.WorkspaceMember;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CreateWorkspaceCommandHandler implements CommandHandler<CreateWorkspaceCommand, CreateWorkspaceResult> {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final TenantContract tenantContract;
    private final UserContract userContract;
    private final ClockPort clockPort;

    @Override
    @Transactional
    public CreateWorkspaceResult handle(CreateWorkspaceCommand command) {
        validateActiveTenant(command.tenantId());
        validateActiveOwner(command.ownerId(), command.tenantId());

        Instant now = clockPort.now();
        Workspace workspace = Workspace.create(command.tenantId(), command.name(), command.ownerId(), now);
        PersistedWorkspace savedWorkspace = workspaceRepository.append(
                new PersistedWorkspace(workspace, WorkspacePersistenceMapper.toState(workspace, null))
        );

        WorkspaceMember owner = WorkspaceMember.joinOwner(
                command.tenantId(),
                savedWorkspace.state().id(),
                command.ownerId(),
                now
        );
        PersistedWorkspaceMember savedOwner = workspaceMemberRepository.append(
                new PersistedWorkspaceMember(owner, WorkspaceMemberPersistenceMapper.toState(owner, null))
        );

        return new CreateWorkspaceResult(
                savedWorkspace.state().id(),
                savedWorkspace.state().tenantId(),
                savedWorkspace.state().name(),
                savedWorkspace.state().status().name(),
                savedOwner.state().userId(),
                savedWorkspace.state().createdAt()
        );
    }

    private void validateActiveTenant(UUID tenantId) {
        if (!tenantContract.existsActiveTenant(tenantId)) {
            throw new InvalidInputException("활성 tenant가 아닙니다");
        }
    }

    private void validateActiveOwner(UUID ownerId, UUID tenantId) {
        if (!userContract.existsUserInTenant(ownerId, tenantId)) {
            throw new InvalidInputException("workspace owner는 활성 tenant user여야 합니다");
        }
    }
}
