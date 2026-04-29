package com.polarishb.pabal.common.persistence.jpa;

import org.hibernate.annotations.IdGeneratorType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD})
@IdGeneratorType(UuidV7IdGenerator.class)
public @interface UuidV7Generated {
    Mode mode() default Mode.MONOTONIC;

    enum Mode { RANDOM, MONOTONIC }
}
