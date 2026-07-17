package com.polarishb.pabal.workspace.infrastructure.persistence;

import com.polarishb.pabal.support.AbstractWorkspacePostgresDataJpaTest;
import com.polarishb.pabal.workspace.application.port.out.persistence.WorkspaceMemberRepository;
import com.polarishb.pabal.workspace.application.port.out.persistence.WorkspaceRepository;
import com.polarishb.pabal.workspace.contract.persistence.PersistedWorkspace;
import com.polarishb.pabal.workspace.contract.persistence.PersistedWorkspaceMember;
import com.polarishb.pabal.workspace.contract.persistence.WorkspaceMemberPersistenceMapper;
import com.polarishb.pabal.workspace.contract.persistence.WorkspacePersistenceMapper;
import com.polarishb.pabal.workspace.contract.persistence.WorkspaceState;
import com.polarishb.pabal.workspace.domain.model.Workspace;
import com.polarishb.pabal.workspace.domain.model.WorkspaceMember;
import com.polarishb.pabal.workspace.domain.model.snapshot.WorkspaceSnapshot;
import com.polarishb.pabal.workspace.domain.model.type.WorkspaceRole;
import com.polarishb.pabal.workspace.domain.model.type.WorkspaceStatus;
import com.polarishb.pabal.workspace.domain.model.vo.WorkspaceName;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.orm.ObjectRetrievalFailureException;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkspaceRepositoryImplTest extends AbstractWorkspacePostgresDataJpaTest {

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private WorkspaceMemberRepository workspaceMemberRepository;

    @Autowired
    private EntityManager entityManager;

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
        assertThat(workspaceMemberRepository.findActiveRole(
                tenantId,
                savedWorkspace.state().id(),
                ownerId
        )).contains(WorkspaceRole.OWNER);
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

    @Test
    void update_rebound_archived_workspace_persists_mutable_fields_and_advances_version() {
        UUID tenantId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-06-19T00:00:00Z");
        Instant archivedAt = Instant.parse("2026-06-19T02:00:00Z");
        PersistedWorkspace saved = appendWorkspace(tenantId, ownerId, "Engineering", createdAt);
        Workspace archived = workspaceWith(
                saved,
                saved.state().tenantId(),
                "Engineering Archive",
                WorkspaceStatus.ARCHIVED,
                archivedAt
        );

        PersistedWorkspace updated = workspaceRepository.update(saved.withWorkspace(archived));

        flushAndClear();
        PersistedWorkspace reread = workspaceRepository.findByTenantIdAndId(
                tenantId,
                saved.state().id()
        ).orElseThrow();
        assertThat(updated.state().id()).isEqualTo(saved.state().id());
        assertThat(updated.state().tenantId()).isEqualTo(tenantId);
        assertThat(updated.state().name()).isEqualTo("Engineering Archive");
        assertThat(updated.state().status()).isEqualTo(WorkspaceStatus.ARCHIVED);
        assertThat(updated.state().updatedAt()).isEqualTo(archivedAt);
        assertThat(updated.state().version()).isGreaterThan(saved.state().version());
        assertThat(reread.state().id()).isEqualTo(saved.state().id());
        assertThat(reread.state().tenantId()).isEqualTo(tenantId);
        assertThat(reread.state().createdBy()).isEqualTo(ownerId);
        assertThat(reread.state().createdAt()).isEqualTo(createdAt);
        assertThat(reread.state().name()).isEqualTo("Engineering Archive");
        assertThat(reread.state().status()).isEqualTo(WorkspaceStatus.ARCHIVED);
        assertThat(reread.state().updatedAt()).isEqualTo(archivedAt);
        assertThat(reread.state().version()).isGreaterThan(saved.state().version());
    }

    @Test
    void update_rejects_stale_workspace_baseline_and_leaves_first_successful_state_unchanged() {
        UUID tenantId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-06-20T00:00:00Z");
        Instant firstUpdatedAt = Instant.parse("2026-06-20T01:00:00Z");
        Instant staleUpdatedAt = Instant.parse("2026-06-20T02:00:00Z");
        PersistedWorkspace saved = appendWorkspace(tenantId, ownerId, "Engineering", createdAt);
        Workspace firstArchive = workspaceWith(
                saved,
                saved.state().tenantId(),
                "Engineering Archive",
                WorkspaceStatus.ARCHIVED,
                firstUpdatedAt
        );
        PersistedWorkspace firstUpdated = workspaceRepository.update(saved.withWorkspace(firstArchive));
        flushAndClear();
        Workspace staleArchive = workspaceWith(
                saved,
                saved.state().tenantId(),
                "Stale Archive",
                WorkspaceStatus.ARCHIVED,
                staleUpdatedAt
        );

        assertThatThrownBy(() -> workspaceRepository.update(saved.withWorkspace(staleArchive)))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);

        flushAndClear();
        PersistedWorkspace reread = workspaceRepository.findByTenantIdAndId(
                tenantId,
                saved.state().id()
        ).orElseThrow();
        assertThat(reread.state().name()).isEqualTo("Engineering Archive");
        assertThat(reread.state().status()).isEqualTo(WorkspaceStatus.ARCHIVED);
        assertThat(reread.state().updatedAt()).isEqualTo(firstUpdatedAt);
        assertThat(reread.state().version()).isEqualTo(firstUpdated.state().version());
    }

    @Test
    void update_with_wrong_tenant_qualified_identity_fails_without_inserting_or_crossing_tenant() {
        UUID tenantId = UUID.randomUUID();
        UUID wrongTenantId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-06-21T00:00:00Z");
        Instant attemptedUpdatedAt = Instant.parse("2026-06-21T01:00:00Z");
        PersistedWorkspace saved = appendWorkspace(tenantId, ownerId, "Engineering", createdAt);
        PersistedWorkspace wrongTenantUpdate = persistedWorkspaceForTenant(
                saved,
                wrongTenantId,
                "Cross Tenant Archive",
                WorkspaceStatus.ARCHIVED,
                attemptedUpdatedAt
        );

        assertThatThrownBy(() -> workspaceRepository.update(wrongTenantUpdate))
                .isInstanceOfAny(
                        EntityNotFoundException.class,
                        EmptyResultDataAccessException.class,
                        ObjectRetrievalFailureException.class
                );

        flushAndClear();
        PersistedWorkspace reread = workspaceRepository.findByTenantIdAndId(
                tenantId,
                saved.state().id()
        ).orElseThrow();
        assertThat(workspaceRowCount()).isEqualTo(1L);
        assertThat(workspaceRepository.findByTenantIdAndId(wrongTenantId, saved.state().id())).isEmpty();
        assertThat(reread.state().tenantId()).isEqualTo(tenantId);
        assertThat(reread.state().name()).isEqualTo("Engineering");
        assertThat(reread.state().status()).isEqualTo(WorkspaceStatus.ACTIVE);
        assertThat(reread.state().updatedAt()).isEqualTo(createdAt);
        assertThat(reread.state().version()).isEqualTo(saved.state().version());
    }

    private PersistedWorkspace appendWorkspace(UUID tenantId, UUID ownerId, String name, Instant createdAt) {
        Workspace workspace = Workspace.create(tenantId, name, ownerId, createdAt);
        return workspaceRepository.append(
                new PersistedWorkspace(workspace, WorkspacePersistenceMapper.toState(workspace, null))
        );
    }

    private PersistedWorkspace persistedWorkspaceForTenant(
            PersistedWorkspace baseline,
            UUID tenantId,
            String name,
            WorkspaceStatus status,
            Instant updatedAt
    ) {
        WorkspaceState state = baseline.state();
        WorkspaceState tenantBaseline = new WorkspaceState(
                state.id(),
                tenantId,
                state.name(),
                state.status(),
                state.createdBy(),
                state.createdAt(),
                state.updatedAt(),
                state.version()
        );
        Workspace desired = workspaceWith(baseline, tenantId, name, status, updatedAt);
        return new PersistedWorkspace(desired, tenantBaseline);
    }

    private Workspace workspaceWith(
            PersistedWorkspace persisted,
            UUID tenantId,
            String name,
            WorkspaceStatus status,
            Instant updatedAt
    ) {
        WorkspaceState state = persisted.state();
        return Workspace.reconstitute(new WorkspaceSnapshot(
                state.id(),
                tenantId,
                new WorkspaceName(name),
                status,
                state.createdBy(),
                state.createdAt(),
                updatedAt
        ));
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    private long workspaceRowCount() {
        return entityManager.createQuery("select count(w) from WorkspaceEntity w", Long.class)
                .getSingleResult();
    }
}
