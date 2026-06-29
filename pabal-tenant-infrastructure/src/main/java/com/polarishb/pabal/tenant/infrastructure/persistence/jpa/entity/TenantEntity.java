package com.polarishb.pabal.tenant.infrastructure.persistence.jpa.entity;

import com.polarishb.pabal.persistence.entity.base.UpdatableEntity;
import com.polarishb.pabal.persistence.jpa.UuidV7Generated;
import com.polarishb.pabal.tenant.contract.persistence.TenantState;
import com.polarishb.pabal.tenant.domain.model.type.TenantStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "pabal_tenant")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TenantEntity extends UpdatableEntity {

    @Id
    @UuidV7Generated(mode = UuidV7Generated.Mode.MONOTONIC)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TenantStatus status;

    @Version
    @Column(nullable = false)
    private Long version;

    public static TenantEntity fromNewState(TenantState state) {
        TenantEntity entity = new TenantEntity();
        entity.id = state.id();
        entity.name = state.name();
        entity.status = state.status();
        entity.setCreatedAt(state.createdAt());
        entity.setUpdatedAt(state.updatedAt());
        return entity;
    }

    public TenantState toState() {
        return new TenantState(
                this.id,
                this.name,
                this.status,
                this.getCreatedAt(),
                this.getUpdatedAt(),
                this.version
        );
    }
}
