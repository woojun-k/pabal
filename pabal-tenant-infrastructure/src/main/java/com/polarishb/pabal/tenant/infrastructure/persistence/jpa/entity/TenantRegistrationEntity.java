package com.polarishb.pabal.tenant.infrastructure.persistence.jpa.entity;

import com.polarishb.pabal.persistence.entity.base.UpdatableEntity;
import com.polarishb.pabal.persistence.jpa.UuidV7Generated;
import com.polarishb.pabal.tenant.contract.persistence.TenantRegistrationState;
import com.polarishb.pabal.tenant.domain.model.type.TenantRegistrationStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tenant_registration")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TenantRegistrationEntity extends UpdatableEntity {

    @Id
    @UuidV7Generated(mode = UuidV7Generated.Mode.MONOTONIC)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String tenantName;

    @Column(nullable = false, length = 253)
    private String domainName;

    @Column(nullable = false, length = 128)
    private String verificationToken;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TenantRegistrationStatus status;

    @Column(nullable = false)
    private Instant expiresAt;

    private Instant verifiedAt;

    private Instant activatedAt;

    private UUID tenantId;

    @Version
    @Column(nullable = false)
    private Long version;

    public static TenantRegistrationEntity fromNewState(TenantRegistrationState state) {
        TenantRegistrationEntity entity = new TenantRegistrationEntity();
        entity.id = state.id();
        entity.tenantName = state.tenantName();
        entity.domainName = state.domainName();
        entity.verificationToken = state.verificationToken();
        entity.status = state.status();
        entity.expiresAt = state.expiresAt();
        entity.verifiedAt = state.verifiedAt();
        entity.activatedAt = state.activatedAt();
        entity.tenantId = state.tenantId();
        entity.setCreatedAt(state.createdAt());
        entity.setUpdatedAt(state.updatedAt());
        return entity;
    }

    public TenantRegistrationState toState() {
        return new TenantRegistrationState(
                this.id,
                this.tenantName,
                this.domainName,
                this.verificationToken,
                this.status,
                this.expiresAt,
                this.verifiedAt,
                this.activatedAt,
                this.tenantId,
                this.getCreatedAt(),
                this.getUpdatedAt(),
                this.version
        );
    }

    public void apply(TenantRegistrationState state) {
        this.tenantName = state.tenantName();
        this.domainName = state.domainName();
        this.verificationToken = state.verificationToken();
        this.status = state.status();
        this.expiresAt = state.expiresAt();
        this.verifiedAt = state.verifiedAt();
        this.activatedAt = state.activatedAt();
        this.tenantId = state.tenantId();
        this.setUpdatedAt(state.updatedAt());
    }
}
