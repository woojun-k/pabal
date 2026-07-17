package com.polarishb.pabal.workspace.infrastructure.persistence.jpa;

import com.polarishb.pabal.workspace.domain.model.type.WorkspaceMemberStatus;
import com.polarishb.pabal.workspace.domain.model.type.WorkspaceRole;
import com.polarishb.pabal.workspace.infrastructure.persistence.jpa.entity.WorkspaceMemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface WorkspaceMemberJpaRepository extends JpaRepository<WorkspaceMemberEntity, UUID> {

    Optional<WorkspaceMemberEntity> findByTenantIdAndId(UUID tenantId, UUID id);

    boolean existsByTenantIdAndWorkspaceIdAndUserIdAndStatus(
            UUID tenantId,
            UUID workspaceId,
            UUID userId,
            WorkspaceMemberStatus status
    );

    @Query("""
            select workspaceMember.userId
            from WorkspaceMemberEntity workspaceMember
            where workspaceMember.tenantId = :tenantId
              and workspaceMember.workspaceId = :workspaceId
              and workspaceMember.userId in :userIds
              and workspaceMember.status = :status
            """)
    Set<UUID> findUserIdsByTenantIdAndWorkspaceIdAndUserIdInAndStatus(
            @Param("tenantId") UUID tenantId,
            @Param("workspaceId") UUID workspaceId,
            @Param("userIds") Set<UUID> userIds,
            @Param("status") WorkspaceMemberStatus status
    );

    @Query("""
            select workspaceMember.role
            from WorkspaceMemberEntity workspaceMember
            where workspaceMember.tenantId = :tenantId
              and workspaceMember.workspaceId = :workspaceId
              and workspaceMember.userId = :userId
              and workspaceMember.status = :status
            """)
    Optional<WorkspaceRole> findRoleByTenantIdAndWorkspaceIdAndUserIdAndStatus(
            @Param("tenantId") UUID tenantId,
            @Param("workspaceId") UUID workspaceId,
            @Param("userId") UUID userId,
            @Param("status") WorkspaceMemberStatus status
    );
}
