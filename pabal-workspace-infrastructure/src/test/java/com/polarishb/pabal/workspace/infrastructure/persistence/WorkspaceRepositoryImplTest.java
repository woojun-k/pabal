package com.polarishb.pabal.workspace.infrastructure.persistence;

import com.polarishb.pabal.support.AbstractWorkspacePostgresDataJpaTest;
import com.polarishb.pabal.workspace.application.port.out.persistence.WorkspaceMemberRepository;
import com.polarishb.pabal.workspace.application.port.out.persistence.WorkspaceRepository;
import com.polarishb.pabal.workspace.contract.persistence.PersistedWorkspace;
import com.polarishb.pabal.workspace.contract.persistence.PersistedWorkspaceMember;
import com.polarishb.pabal.workspace.contract.persistence.WorkspaceMemberPersistenceMapper;
import com.polarishb.pabal.workspace.contract.persistence.WorkspacePersistenceMapper;
import com.polarishb.pabal.workspace.domain.model.Workspace;
import com.polarishb.pabal.workspace.domain.model.WorkspaceMember;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class WorkspaceRepositoryImplTest extends AbstractWorkspacePostgresDataJpaTest {

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private WorkspaceMemberRepository workspaceMemberRepository;

    @Test
    void append_and_findByTenantIdAndId_round_trips_active_workspace_and_owner_member() {
        UUID tenantId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID tenantUserId = UUID.randomUUID();
        UUID missingUserId = UUID.randomUUID();
        Instant now = Instant.parse("2026-04-08T00:00:00Z");
        Workspace workspace = Workspace.create(tenantId, "Engineering", ownerId, now);

        PersistedWorkspace savedWorkspace = workspaceRepository.append(
                new PersistedWorkspace(workspace, WorkspacePersistenceMapper.toState(workspace, null))
        );
        WorkspaceMember owner = WorkspaceMember.joinOwner(
                tenantId,
                savedWorkspace.state().id(),
                ownerId,
                now
        );
        PersistedWorkspaceMember savedMember = workspaceMemberRepository.append(
                new PersistedWorkspaceMember(owner, WorkspaceMemberPersistenceMapper.toState(owner, null))
        );
        Optional<PersistedWorkspace> found = workspaceRepository.findByTenantIdAndId(
                tenantId,
                savedWorkspace.state().id()
        );

        assertThat(savedWorkspace.state().id()).isNotNull();
        assertThat(savedWorkspace.state().version()).isEqualTo(0L);
        assertThat(savedMember.state().id()).isNotNull();
        assertThat(savedMember.state().workspaceId()).isEqualTo(savedWorkspace.state().id());
        assertThat(found).isPresent();
        assertThat(workspaceRepository.existsActiveByTenantIdAndId(tenantId, savedWorkspace.state().id())).isTrue();
        assertThat(workspaceMemberRepository.existsActiveMember(
                tenantId,
                savedWorkspace.state().id(),
                ownerId
        )).isTrue();
        assertThat(workspaceMemberRepository.findActiveUserIds(
                tenantId,
                savedWorkspace.state().id(),
                Set.of(ownerId, tenantUserId, missingUserId)
        )).containsExactly(ownerId);
    }

    @Test
    void findActiveUserIds_returns_empty_without_querying_empty_id_set() {
        UUID tenantId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();

        Set<UUID> userIds = workspaceMemberRepository.findActiveUserIds(tenantId, workspaceId, Set.of());

        assertThat(userIds).isEmpty();
    }
}
