package com.polarishb.pabal.user.contract.persistence;

import com.polarishb.pabal.user.domain.model.snapshot.UserSnapshot;
import com.polarishb.pabal.user.domain.model.type.UserStatus;
import com.polarishb.pabal.user.domain.model.vo.UserName;

import java.time.Instant;
import java.util.UUID;

public record UserState(
        UUID id,
        UUID tenantId,
        String name,
        UserStatus status,
        Instant createdAt,
        Instant updatedAt,
        Long version
) {
    public UserState(
            UserSnapshot snapshot,
            Long version
    ) {
        this(
                snapshot.id(),
                snapshot.tenantId(),
                snapshot.name().value(),
                snapshot.status(),
                snapshot.createdAt(),
                snapshot.updatedAt(),
                version
        );
    }

    public UserSnapshot snapshot() {
        return new UserSnapshot(
                id,
                tenantId,
                new UserName(name),
                status,
                createdAt,
                updatedAt
        );
    }
}
