package com.polarishb.pabal.workspace.application.query.handler;

import com.polarishb.pabal.workspace.application.port.out.persistence.WorkspaceRepository;
import com.polarishb.pabal.workspace.application.query.input.GetWorkspaceQuery;
import com.polarishb.pabal.workspace.application.query.output.WorkspaceDto;
import com.polarishb.pabal.workspace.contract.persistence.PersistedWorkspace;
import com.polarishb.pabal.workspace.contract.persistence.WorkspacePersistenceMapper;
import com.polarishb.pabal.workspace.contract.persistence.WorkspaceState;
import com.polarishb.pabal.workspace.domain.exception.WorkspaceNotFoundException;
import com.polarishb.pabal.workspace.domain.model.type.WorkspaceStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class GetWorkspaceQueryHandlerTest {

    private static final Instant CREATED_AT = Instant.parse("2026-06-19T00:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-06-19T00:05:00Z");

    private final WorkspaceRepository workspaceRepository = mock(WorkspaceRepository.class);

    private GetWorkspaceQueryHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GetWorkspaceQueryHandler(workspaceRepository);
    }

    @Test
    void handle_reads_workspace_through_tenant_scoped_repository_lookup() {
        UUID tenantId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        when(workspaceRepository.findByTenantIdAndId(tenantId, workspaceId))
                .thenReturn(Optional.of(persistedWorkspace(tenantId, workspaceId, ownerId)));

        WorkspaceDto result = handler.handle(new GetWorkspaceQuery(tenantId, workspaceId));

        assertThat(result.workspaceId()).isEqualTo(workspaceId);
        assertThat(result.tenantId()).isEqualTo(tenantId);
        assertThat(result.name()).isEqualTo("Engineering");
        assertThat(result.status()).isEqualTo("ACTIVE");
        assertThat(result.createdBy()).isEqualTo(ownerId);

        verify(workspaceRepository).findByTenantIdAndId(tenantId, workspaceId);
    }

    @Test
    void handle_does_not_return_workspace_when_same_id_is_not_in_requested_tenant() {
        UUID requestedTenantId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        when(workspaceRepository.findByTenantIdAndId(requestedTenantId, workspaceId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.handle(new GetWorkspaceQuery(requestedTenantId, workspaceId)))
                .isInstanceOf(WorkspaceNotFoundException.class);

        verify(workspaceRepository).findByTenantIdAndId(requestedTenantId, workspaceId);
    }

    private PersistedWorkspace persistedWorkspace(UUID tenantId, UUID workspaceId, UUID ownerId) {
        WorkspaceState state = new WorkspaceState(
                workspaceId,
                tenantId,
                "Engineering",
                WorkspaceStatus.ACTIVE,
                ownerId,
                CREATED_AT,
                UPDATED_AT,
                0L
        );
        return WorkspacePersistenceMapper.toPersisted(state);
    }
}
