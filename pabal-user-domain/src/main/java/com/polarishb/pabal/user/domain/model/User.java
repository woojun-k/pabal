package com.polarishb.pabal.user.domain.model;

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
    private UUID id;
    private UUID tenantId;
    private UserName name;
    private UserStatus status;
    private Instant createdAt;
    private Instant updatedAt;

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

    public void rename(String newName, Instant updatedAt) {
        this.name = new UserName(newName);
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public void disable(Instant updatedAt) {
        this.status = UserStatus.DISABLED;
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }
}
