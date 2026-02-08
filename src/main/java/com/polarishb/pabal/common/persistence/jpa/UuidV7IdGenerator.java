package com.polarishb.pabal.common.persistence.jpa;

import com.polarishb.pabal.common.util.UuidV7;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.generator.EventTypeSets;
import org.hibernate.generator.GeneratorCreationContext;

import java.lang.reflect.Member;
import java.util.EnumSet;

public class UuidV7IdGenerator implements BeforeExecutionGenerator {

    private final UuidV7Generated.Mode mode;

    public UuidV7IdGenerator(UuidV7Generated config, Member annotatedMember, GeneratorCreationContext context) {
        this.mode = (config == null) ? UuidV7Generated.Mode.MONOTONIC : config.mode();
    }

    @Override
    public Object generate(SharedSessionContractImplementor session, Object owner, Object currentValue, EventType eventType) {
        if (currentValue != null) return currentValue;

        return (mode == UuidV7Generated.Mode.RANDOM)
                ? UuidV7.random()
                : UuidV7.monotonic();
    }

    @Override
    public EnumSet<EventType> getEventTypes() {
        return EventTypeSets.INSERT_ONLY;
    }

    @Override
    public boolean allowAssignedIdentifiers() {
        return true;
    }

}
