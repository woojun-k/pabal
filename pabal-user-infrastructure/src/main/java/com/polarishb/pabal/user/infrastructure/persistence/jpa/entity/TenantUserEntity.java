package com.polarishb.pabal.user.infrastructure.persistence.jpa.entity;

import com.polarishb.pabal.common.persistence.entity.base.UpdatableEntity;
import com.polarishb.pabal.user.contract.persistence.UserState;
import com.polarishb.pabal.user.domain.model.type.UserStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "tenant_user")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TenantUserEntity extends UpdatableEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    @Version
    @Column(nullable = false)
    private Long version;

    public static TenantUserEntity fromState(UserState state) {
        TenantUserEntity entity = new TenantUserEntity();
        entity.id = state.id();
        entity.tenantId = state.tenantId();
        entity.name = state.name();
        entity.status = state.status();
        entity.setCreatedAt(state.createdAt());
        entity.setUpdatedAt(state.updatedAt());
        return entity;
    }

    public UserState toState() {
        return new UserState(
                this.id,
                this.tenantId,
                this.name,
                this.status,
                this.getCreatedAt(),
                this.getUpdatedAt(),
                this.version
        );
    }
}
