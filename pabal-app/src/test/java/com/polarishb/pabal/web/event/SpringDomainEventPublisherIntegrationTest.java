package com.polarishb.pabal.web.event;

import com.polarishb.pabal.common.event.DomainEvent;
import com.polarishb.pabal.common.event.DomainEventPublisher;
import com.polarishb.pabal.support.PabalSpringBootIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

@PabalSpringBootIntegrationTest
@Import(SpringDomainEventPublisherIntegrationTest.TestConfig.class)
class SpringDomainEventPublisherIntegrationTest {

    @Autowired
    private DomainEventPublisher domainEventPublisher;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private RecordingListener recordingListener;

    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        transactionTemplate = new TransactionTemplate(transactionManager);
        recordingListener.reset();
    }

    @Test
    void publishAfterCommit_dispatches_event_after_transaction_commit() {
        AtomicBoolean notPublishedInsideTransaction = new AtomicBoolean(false);

        transactionTemplate.executeWithoutResult(status -> {
            domainEventPublisher.publishAfterCommit(new TestDomainEvent("after-commit"));
            notPublishedInsideTransaction.set(recordingListener.values().isEmpty());
        });

        assertThat(notPublishedInsideTransaction).isTrue();
        assertThat(recordingListener.values()).containsExactly("after-commit");
    }

    private record TestDomainEvent(String message) implements DomainEvent {
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        RecordingListener recordingListener() {
            return new RecordingListener();
        }
    }

    static class RecordingListener {

        private final List<String> values = new CopyOnWriteArrayList<>();

        @EventListener
        public void handle(TestDomainEvent event) {
            values.add(event.message());
        }

        List<String> values() {
            return List.copyOf(values);
        }

        void reset() {
            values.clear();
        }
    }
}
