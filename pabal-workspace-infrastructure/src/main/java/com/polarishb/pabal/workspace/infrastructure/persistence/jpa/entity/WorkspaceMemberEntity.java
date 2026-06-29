package com.polarishb.pabal.workspace.infrastructure.persistence.jpa.entity;

import com.polarishb.pabal.persistence.entity.base.UpdatableEntity;
import com.polarishb.pabal.persistence.jpa.UuidV7Generated;
import com.polarishb.pabal.workspace.contract.persistence.WorkspaceMemberState;
import com.polarishb.pabal.workspace.domain.model.type.WorkspaceMemberStatus;
import com.polarishb.pabal.workspace.domain.model.type.WorkspaceRole;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "workspace_member")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkspaceMemberEntity extends UpdatableEntity {

    @Id
    @UuidV7Generated(mode = UuidV7Generated.Mode.MONOTONIC)
    private UUID id;

    @Column(nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private UUID workspaceId;

    @Column(nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WorkspaceRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WorkspaceMemberStatus status;

    @Column(nullable = false)
    private Instant joinedAt;

    private Instant leftAt;

    @Version
    @Column(nullable = false)
    private Long version;

    public static WorkspaceMemberEntity fromNewState(WorkspaceMemberState state) {
        WorkspaceMemberEntity entity = new WorkspaceMemberEntity();
        entity.id = state.id();
        entity.tenantId = state.tenantId();
        entity.workspaceId = state.workspaceId();
        entity.userId = state.userId();
        entity.role = state.role();
        entity.status = state.status();
        entity.joinedAt = state.joinedAt();
        entity.leftAt = state.leftAt();
        entity.setCreatedAt(state.createdAt());
        entity.setUpdatedAt(state.updatedAt());
        return entity;
    }

    public WorkspaceMemberState toState() {
        return new WorkspaceMemberState(
                this.id,
                this.tenantId,
                this.workspaceId,
                this.userId,
                this.role,
                this.status,
                this.joinedAt,
                this.leftAt,
                this.getCreatedAt(),
                this.getUpdatedAt(),
                this.version
        );
    }
}
