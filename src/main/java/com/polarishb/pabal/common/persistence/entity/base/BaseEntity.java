package com.polarishb.pabal.common.persistence.entity.base;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

import java.time.Instant;

@MappedSuperclass
@Getter
public abstract class BaseEntity {

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected final void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}