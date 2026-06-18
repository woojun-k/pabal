package com.polarishb.pabal.user.contract.persistence;

import com.polarishb.pabal.user.domain.model.snapshot.UserSnapshot;
import com.polarishb.pabal.user.domain.model.type.UserStatus;
import com.polarishb.pabal.user.domain.model.vo.UserName;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record UserState(
        UserSnapshot snapshot,
        Long version
) {
    public UserState {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
    }

    public UserState(
            UUID id,
            UUID tenantId,
            String name,
            UserStatus status,
            Instant createdAt,
            Instant updatedAt,
            Long version
    ) {
        this(
                new UserSnapshot(
                        id,
                        tenantId,
                        new UserName(name),
                        status,
                        createdAt,
                        updatedAt
                ),
                version
        );
    }

    public UUID id() {
        return snapshot.id();
    }

    public UUID tenantId() {
        return snapshot.tenantId();
    }

    public String name() {
        return snapshot.name().value();
    }

    public UserStatus status() {
        return snapshot.status();
    }

    public Instant createdAt() {
        return snapshot.createdAt();
    }

    public Instant updatedAt() {
        return snapshot.updatedAt();
    }
}
