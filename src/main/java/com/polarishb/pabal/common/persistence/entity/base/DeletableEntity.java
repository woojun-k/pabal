package com.polarishb.pabal.common.persistence.entity.base;

import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

import java.time.Instant;

@MappedSuperclass
@Getter
public abstract class DeletableEntity extends UpdatableEntity {

    private Instant deletedAt;

    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    protected final void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }
}