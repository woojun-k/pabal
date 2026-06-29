package com.polarishb.pabal.workspace.application.query.handler;

import com.polarishb.pabal.common.cqrs.QueryHandler;
import com.polarishb.pabal.workspace.application.port.out.persistence.WorkspaceRepository;
import com.polarishb.pabal.workspace.application.query.input.GetWorkspaceQuery;
import com.polarishb.pabal.workspace.application.query.output.WorkspaceDto;
import com.polarishb.pabal.workspace.contract.persistence.PersistedWorkspace;
import com.polarishb.pabal.workspace.domain.exception.WorkspaceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class GetWorkspaceQueryHandler implements QueryHandler<GetWorkspaceQuery, WorkspaceDto> {

    private final WorkspaceRepository workspaceRepository;

    @Override
    @Transactional(readOnly = true)
    public WorkspaceDto handle(GetWorkspaceQuery query) {
        PersistedWorkspace workspace = workspaceRepository.findByTenantIdAndId(query.tenantId(), query.workspaceId())
                .orElseThrow(() -> new WorkspaceNotFoundException(query.workspaceId()));
        return new WorkspaceDto(
                workspace.state().id(),
                workspace.state().tenantId(),
                workspace.state().name(),
                workspace.state().status().name(),
                workspace.state().createdBy(),
                workspace.state().createdAt(),
                workspace.state().updatedAt()
        );
    }
}
