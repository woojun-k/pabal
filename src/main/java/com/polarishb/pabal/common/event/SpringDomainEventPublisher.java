package com.polarishb.pabal.common.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Component
@RequiredArgsConstructor
public class SpringDomainEventPublisher implements DomainEventPublisher{

    private final ApplicationEventPublisher delegate;

    @Override
    public void publishNow(DomainEvent event) {
        log.debug("Publishing event now: {}", event.getClass().getSimpleName());
        delegate.publishEvent(event);
    }

    @Override
    public void publishAfterCommit(DomainEvent event) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()
                || !TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("Actual active transaction required for publishAfterCommit");
        }

        log.debug("Registering event for after-commit: {}", event.getClass().getSimpleName());

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        log.debug("Publishing event after commit: {}", event.getClass().getSimpleName());
                        delegate.publishEvent(event);
                    }
                }
        );
    }
}
