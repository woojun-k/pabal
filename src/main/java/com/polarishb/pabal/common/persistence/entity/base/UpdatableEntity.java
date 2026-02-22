package com.polarishb.pabal.common.persistence.entity.base;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.Instant;

@MappedSuperclass
@Getter
public abstract class UpdatableEntity extends BaseEntity {

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;
}