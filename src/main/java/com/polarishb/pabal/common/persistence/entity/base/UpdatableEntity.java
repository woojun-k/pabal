package com.polarishb.pabal.common.persistence.entity.base;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

import java.time.Instant;

@MappedSuperclass
@Getter
public abstract class UpdatableEntity extends BaseEntity {

    @Column(nullable = false)
    private Instant updatedAt;

    protected final void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}