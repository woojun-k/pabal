package com.polarishb.pabal.tenant.domain.model;

import com.polarishb.pabal.tenant.domain.model.snapshot.TenantSnapshot;
import com.polarishb.pabal.tenant.domain.model.type.TenantStatus;
import com.polarishb.pabal.tenant.domain.model.vo.TenantName;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Tenant {

    @EqualsAndHashCode.Include
    private final UUID id;
    private final TenantName name;
    private final TenantStatus status;
    private final Instant createdAt;
    private final Instant updatedAt;

    public static Tenant create(String name, Instant createdAt) {
        return new Tenant(
                null,
                new TenantName(name),
                TenantStatus.ACTIVE,
                Objects.requireNonNull(createdAt, "createdAt must not be null"),
                createdAt
        );
    }

    public static Tenant reconstitute(TenantSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        return new Tenant(
                snapshot.id(),
                snapshot.name(),
                snapshot.status(),
                snapshot.createdAt(),
                snapshot.updatedAt()
        );
    }

    public TenantSnapshot snapshot() {
        return new TenantSnapshot(id, name, status, createdAt, updatedAt);
    }

    public boolean isActive() {
        return status == TenantStatus.ACTIVE;
    }
}
