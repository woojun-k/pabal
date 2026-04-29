package com.polarishb.pabal.common.event;

public interface EventHandler<E extends DomainEvent> {
    void handle(E event);
}
