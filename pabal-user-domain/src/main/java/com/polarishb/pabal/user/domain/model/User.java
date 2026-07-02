package com.polarishb.pabal.user.domain.model;

import com.polarishb.pabal.user.domain.exception.UserAlreadyDisabledException;
import com.polarishb.pabal.user.domain.model.snapshot.UserSnapshot;
import com.polarishb.pabal.user.domain.model.type.UserStatus;
import com.polarishb.pabal.user.domain.model.vo.UserName;
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
public class User {

    @EqualsAndHashCode.Include
    private final UUID id;
    private final UUID tenantId;
    private final UserName name;
    private final UserStatus status;
    private final Instant createdAt;
    private final Instant updatedAt;

    public static User create(UUID id, UUID tenantId, String name, Instant createdAt) {
        return new User(
                Objects.requireNonNull(id, "id must not be null"),
                Objects.requireNonNull(tenantId, "tenantId must not be null"),
                new UserName(name),
                UserStatus.ACTIVE,
                Objects.requireNonNull(createdAt, "createdAt must not be null"),
                createdAt
        );
    }

    public static User reconstitute(UserSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        return new User(
                snapshot.id(),
                snapshot.tenantId(),
                snapshot.name(),
                snapshot.status(),
                snapshot.createdAt(),
                snapshot.updatedAt()
        );
    }

    public UserSnapshot snapshot() {
        return new UserSnapshot(
                id,
                tenantId,
                name,
                status,
                createdAt,
                updatedAt
        );
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }

    public User rename(String newName, Instant updatedAt) {
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        if (this.status == UserStatus.DISABLED) {
            throw new UserAlreadyDisabledException();
        }
        return new User(
                id,
                tenantId,
                new UserName(newName),
                status,
                createdAt,
                updatedAt
        );
    }

    public User disable(Instant updatedAt) {
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        if (this.status == UserStatus.DISABLED) {
            throw new UserAlreadyDisabledException();
        }
        return new User(
                id,
                tenantId,
                name,
                UserStatus.DISABLED,
                createdAt,
                updatedAt
        );
    }
}
