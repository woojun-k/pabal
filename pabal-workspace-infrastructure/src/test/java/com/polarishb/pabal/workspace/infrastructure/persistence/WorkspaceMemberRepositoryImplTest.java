package com.polarishb.pabal.workspace.infrastructure.persistence;

import com.polarishb.pabal.support.AbstractWorkspacePostgresDataJpaTest;
import com.polarishb.pabal.workspace.application.port.out.persistence.WorkspaceMemberRepository;
import com.polarishb.pabal.workspace.application.port.out.persistence.WorkspaceRepository;
import com.polarishb.pabal.workspace.contract.persistence.PersistedWorkspace;
import com.polarishb.pabal.workspace.contract.persistence.PersistedWorkspaceMember;
import com.polarishb.pabal.workspace.contract.persistence.WorkspaceMemberPersistenceMapper;
import com.polarishb.pabal.workspace.contract.persistence.WorkspaceMemberState;
import com.polarishb.pabal.workspace.contract.persistence.WorkspacePersistenceMapper;
import com.polarishb.pabal.workspace.domain.model.Workspace;
import com.polarishb.pabal.workspace.domain.model.WorkspaceMember;
import com.polarishb.pabal.workspace.domain.model.snapshot.WorkspaceMemberSnapshot;
import com.polarishb.pabal.workspace.domain.model.type.WorkspaceMemberStatus;
import com.polarishb.pabal.workspace.domain.model.type.WorkspaceRole;
import com.polarishb.pabal.workspace.infrastructure.persistence.jpa.WorkspaceMemberJpaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.orm.ObjectRetrievalFailureException;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkspaceMemberRepositoryImplTest extends AbstractWorkspacePostgresDataJpaTest {

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private WorkspaceMemberRepository workspaceMemberRepository;

    @Autowired
    private WorkspaceMemberJpaRepository workspaceMemberJpaRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void update_rebound_left_member_persists_left_state_and_updates_active_lookups() {
        UUID tenantId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID memberUserId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-06-22T00:00:00Z");
        Instant leftAt = Instant.parse("2026-06-22T01:00:00Z");
        PersistedWorkspace workspace = appendWorkspace(tenantId, ownerId, createdAt);
        PersistedWorkspaceMember saved = appendMember(
                tenantId,
                workspace.state().id(),
                memberUserId,
                WorkspaceRole.MEMBER,
                createdAt
        );
        WorkspaceMember leftMember = saved.member().leave(leftAt, true);

        PersistedWorkspaceMember updated = workspaceMemberRepository.update(saved.withMember(leftMember));

        flushAndClear();
        WorkspaceMemberState reread = findMemberState(saved.state().id());
        assertThat(updated.state().id()).isEqualTo(saved.state().id());
        assertThat(updated.state().tenantId()).isEqualTo(tenantId);
        assertThat(updated.state().workspaceId()).isEqualTo(workspace.state().id());
        assertThat(updated.state().userId()).isEqualTo(memberUserId);
        assertThat(updated.state().role()).isEqualTo(WorkspaceRole.MEMBER);
        assertThat(updated.state().status()).isEqualTo(WorkspaceMemberStatus.LEFT);
        assertThat(updated.state().leftAt()).isEqualTo(leftAt);
        assertThat(updated.state().updatedAt()).isEqualTo(leftAt);
        assertThat(updated.state().version()).isGreaterThan(saved.state().version());
        assertThat(reread.id()).isEqualTo(saved.state().id());
        assertThat(reread.tenantId()).isEqualTo(tenantId);
        assertThat(reread.workspaceId()).isEqualTo(workspace.state().id());
        assertThat(reread.userId()).isEqualTo(memberUserId);
        assertThat(reread.role()).isEqualTo(WorkspaceRole.MEMBER);
        assertThat(reread.status()).isEqualTo(WorkspaceMemberStatus.LEFT);
        assertThat(reread.leftAt()).isEqualTo(leftAt);
        assertThat(reread.updatedAt()).isEqualTo(leftAt);
        assertThat(reread.version()).isGreaterThan(saved.state().version());
        assertThat(workspaceMemberRepository.existsActiveMember(
                tenantId,
                workspace.state().id(),
                memberUserId
        )).isFalse();
        assertThat(workspaceMemberRepository.findActiveRole(
                tenantId,
                workspace.state().id(),
                memberUserId
        )).isEmpty();
        assertThat(workspaceMemberRepository.findActiveUserIds(
                tenantId,
                workspace.state().id(),
                Set.of(memberUserId)
        )).isEmpty();
    }

    @Test
    void update_rebound_member_role_preserves_membership_identity() {
        UUID tenantId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID memberUserId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-06-23T00:00:00Z");
        Instant changedAt = Instant.parse("2026-06-23T01:00:00Z");
        PersistedWorkspace workspace = appendWorkspace(tenantId, ownerId, createdAt);
        PersistedWorkspaceMember saved = appendMember(
                tenantId,
                workspace.state().id(),
                memberUserId,
                WorkspaceRole.MEMBER,
                createdAt
        );
        WorkspaceMember changedRole = saved.member().changeRole(WorkspaceRole.ADMIN, changedAt, true);

        PersistedWorkspaceMember updated = workspaceMemberRepository.update(saved.withMember(changedRole));

        flushAndClear();
        WorkspaceMemberState reread = findMemberState(saved.state().id());
        assertThat(updated.state().id()).isEqualTo(saved.state().id());
        assertThat(updated.state().tenantId()).isEqualTo(tenantId);
        assertThat(updated.state().workspaceId()).isEqualTo(workspace.state().id());
        assertThat(updated.state().userId()).isEqualTo(memberUserId);
        assertThat(updated.state().role()).isEqualTo(WorkspaceRole.ADMIN);
        assertThat(updated.state().status()).isEqualTo(WorkspaceMemberStatus.ACTIVE);
        assertThat(updated.state().leftAt()).isNull();
        assertThat(updated.state().updatedAt()).isEqualTo(changedAt);
        assertThat(updated.state().version()).isGreaterThan(saved.state().version());
        assertThat(reread.id()).isEqualTo(saved.state().id());
        assertThat(reread.tenantId()).isEqualTo(tenantId);
        assertThat(reread.workspaceId()).isEqualTo(workspace.state().id());
        assertThat(reread.userId()).isEqualTo(memberUserId);
        assertThat(reread.role()).isEqualTo(WorkspaceRole.ADMIN);
        assertThat(reread.status()).isEqualTo(WorkspaceMemberStatus.ACTIVE);
        assertThat(reread.leftAt()).isNull();
        assertThat(reread.updatedAt()).isEqualTo(changedAt);
    }

    @Test
    void update_rejects_stale_member_baseline_and_leaves_first_successful_state_unchanged() {
        UUID tenantId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID memberUserId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-06-24T00:00:00Z");
        Instant firstChangedAt = Instant.parse("2026-06-24T01:00:00Z");
        Instant staleChangedAt = Instant.parse("2026-06-24T02:00:00Z");
        PersistedWorkspace workspace = appendWorkspace(tenantId, ownerId, createdAt);
        PersistedWorkspaceMember saved = appendMember(
                tenantId,
                workspace.state().id(),
                memberUserId,
                WorkspaceRole.MEMBER,
                createdAt
        );
        WorkspaceMember firstChange = saved.member().changeRole(WorkspaceRole.ADMIN, firstChangedAt, true);
        PersistedWorkspaceMember firstUpdated = workspaceMemberRepository.update(saved.withMember(firstChange));
        flushAndClear();
        WorkspaceMember staleChange = saved.member().changeRole(WorkspaceRole.OWNER, staleChangedAt, true);

        assertThatThrownBy(() -> workspaceMemberRepository.update(saved.withMember(staleChange)))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);

        flushAndClear();
        WorkspaceMemberState reread = findMemberState(saved.state().id());
        assertThat(reread.role()).isEqualTo(WorkspaceRole.ADMIN);
        assertThat(reread.status()).isEqualTo(WorkspaceMemberStatus.ACTIVE);
        assertThat(reread.leftAt()).isNull();
        assertThat(reread.updatedAt()).isEqualTo(firstChangedAt);
        assertThat(reread.version()).isEqualTo(firstUpdated.state().version());
    }

    @Test
    void update_with_wrong_tenant_qualified_identity_fails_without_inserting_or_crossing_tenant() {
        UUID tenantId = UUID.randomUUID();
        UUID wrongTenantId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID memberUserId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-06-25T00:00:00Z");
        Instant attemptedChangedAt = Instant.parse("2026-06-25T01:00:00Z");
        PersistedWorkspace workspace = appendWorkspace(tenantId, ownerId, createdAt);
        PersistedWorkspaceMember saved = appendMember(
                tenantId,
                workspace.state().id(),
                memberUserId,
                WorkspaceRole.MEMBER,
                createdAt
        );
        PersistedWorkspaceMember wrongTenantUpdate = persistedMemberForTenant(
                saved,
                wrongTenantId,
                WorkspaceRole.ADMIN,
                WorkspaceMemberStatus.ACTIVE,
                null,
                attemptedChangedAt
        );

        assertThatThrownBy(() -> workspaceMemberRepository.update(wrongTenantUpdate))
                .isInstanceOfAny(
                        EntityNotFoundException.class,
                        EmptyResultDataAccessException.class,
                        ObjectRetrievalFailureException.class
                );

        flushAndClear();
        WorkspaceMemberState reread = findMemberState(saved.state().id());
        assertThat(workspaceMemberRowCount()).isEqualTo(1L);
        assertThat(reread.tenantId()).isEqualTo(tenantId);
        assertThat(reread.workspaceId()).isEqualTo(workspace.state().id());
        assertThat(reread.userId()).isEqualTo(memberUserId);
        assertThat(reread.role()).isEqualTo(WorkspaceRole.MEMBER);
        assertThat(reread.status()).isEqualTo(WorkspaceMemberStatus.ACTIVE);
        assertThat(reread.leftAt()).isNull();
        assertThat(reread.updatedAt()).isEqualTo(createdAt);
        assertThat(reread.version()).isEqualTo(saved.state().version());
    }

    private PersistedWorkspace appendWorkspace(UUID tenantId, UUID ownerId, Instant createdAt) {
        Workspace workspace = Workspace.create(tenantId, "Engineering", ownerId, createdAt);
        return workspaceRepository.append(
                new PersistedWorkspace(workspace, WorkspacePersistenceMapper.toState(workspace, null))
        );
    }

    private PersistedWorkspaceMember appendMember(
            UUID tenantId,
            UUID workspaceId,
            UUID userId,
            WorkspaceRole role,
            Instant joinedAt
    ) {
        WorkspaceMember member = WorkspaceMember.join(tenantId, workspaceId, userId, role, joinedAt);
        return workspaceMemberRepository.append(
                new PersistedWorkspaceMember(member, WorkspaceMemberPersistenceMapper.toState(member, null))
        );
    }

    private PersistedWorkspaceMember persistedMemberForTenant(
            PersistedWorkspaceMember baseline,
            UUID tenantId,
            WorkspaceRole role,
            WorkspaceMemberStatus status,
            Instant leftAt,
            Instant updatedAt
    ) {
        WorkspaceMemberState state = baseline.state();
        WorkspaceMemberState tenantBaseline = new WorkspaceMemberState(
                state.id(),
                tenantId,
                state.workspaceId(),
                state.userId(),
                state.role(),
                state.status(),
                state.joinedAt(),
                state.leftAt(),
                state.createdAt(),
                state.updatedAt(),
                state.version()
        );
        WorkspaceMember desired = WorkspaceMember.reconstitute(new WorkspaceMemberSnapshot(
                state.id(),
                tenantId,
                state.workspaceId(),
                state.userId(),
                role,
                status,
                state.joinedAt(),
                leftAt,
                state.createdAt(),
                updatedAt
        ));
        return new PersistedWorkspaceMember(desired, tenantBaseline);
    }

    private WorkspaceMemberState findMemberState(UUID memberId) {
        return workspaceMemberJpaRepository.findById(memberId)
                .orElseThrow()
                .toState();
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    private long workspaceMemberRowCount() {
        return entityManager.createQuery("select count(m) from WorkspaceMemberEntity m", Long.class)
                .getSingleResult();
    }
}
