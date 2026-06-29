package com.polarishb.pabal.workspace.infrastructure.persistence.jpa.entity;

import com.polarishb.pabal.persistence.entity.base.UpdatableEntity;
import com.polarishb.pabal.persistence.jpa.UuidV7Generated;
import com.polarishb.pabal.workspace.contract.persistence.WorkspaceState;
import com.polarishb.pabal.workspace.domain.model.type.WorkspaceStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "workspace")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkspaceEntity extends UpdatableEntity {

    @Id
    @UuidV7Generated(mode = UuidV7Generated.Mode.MONOTONIC)
    private UUID id;

    @Column(nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WorkspaceStatus status;

    @Column(nullable = false)
    private UUID createdBy;

    @Version
    @Column(nullable = false)
    private Long version;

    public static WorkspaceEntity fromNewState(WorkspaceState state) {
        WorkspaceEntity entity = new WorkspaceEntity();
        entity.id = state.id();
        entity.tenantId = state.tenantId();
        entity.name = state.name();
        entity.status = state.status();
        entity.createdBy = state.createdBy();
        entity.setCreatedAt(state.createdAt());
        entity.setUpdatedAt(state.updatedAt());
        return entity;
    }

    public WorkspaceState toState() {
        return new WorkspaceState(
                this.id,
                this.tenantId,
                this.name,
                this.status,
                this.createdBy,
                this.getCreatedAt(),
                this.getUpdatedAt(),
                this.version
        );
    }
}
