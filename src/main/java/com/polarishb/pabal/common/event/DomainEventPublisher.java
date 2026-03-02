package com.polarishb.pabal.common.event;

public interface DomainEventPublisher {

    void publishNow(DomainEvent event);

    void publishAfterCommit(DomainEvent event);
}
