package com.polarishb.pabal.workspace.infrastructure.persistence;

import com.polarishb.pabal.workspace.application.port.out.persistence.WorkspaceMemberRepository;
import com.polarishb.pabal.workspace.contract.persistence.PersistedWorkspaceMember;
import com.polarishb.pabal.workspace.contract.persistence.WorkspaceMemberPersistenceMapper;
import com.polarishb.pabal.workspace.domain.model.type.WorkspaceMemberStatus;
import com.polarishb.pabal.workspace.infrastructure.persistence.jpa.WorkspaceMemberJpaRepository;
import com.polarishb.pabal.workspace.infrastructure.persistence.jpa.entity.WorkspaceMemberEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class WorkspaceMemberRepositoryImpl implements WorkspaceMemberRepository {

    private final WorkspaceMemberJpaRepository jpaRepository;

    @Override
    public PersistedWorkspaceMember append(PersistedWorkspaceMember member) {
        WorkspaceMemberEntity saved = jpaRepository.save(WorkspaceMemberEntity.fromNewState(member.state()));
        return WorkspaceMemberPersistenceMapper.toPersisted(saved.toState());
    }

    @Override
    public boolean existsActiveMember(UUID tenantId, UUID workspaceId, UUID userId) {
        return jpaRepository.existsByTenantIdAndWorkspaceIdAndUserIdAndStatus(
                tenantId,
                workspaceId,
                userId,
                WorkspaceMemberStatus.ACTIVE
        );
    }

    @Override
    public Set<UUID> findActiveUserIds(UUID tenantId, UUID workspaceId, Set<UUID> userIds) {
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        Objects.requireNonNull(userIds, "userIds must not be null");
        if (userIds.isEmpty()) {
            return Set.of();
        }

        return Set.copyOf(jpaRepository.findUserIdsByTenantIdAndWorkspaceIdAndUserIdInAndStatus(
                tenantId,
                workspaceId,
                userIds,
                WorkspaceMemberStatus.ACTIVE
        ));
    }
}
