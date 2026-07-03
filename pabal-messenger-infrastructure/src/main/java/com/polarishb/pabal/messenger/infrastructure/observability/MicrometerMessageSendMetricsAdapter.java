package com.polarishb.pabal.messenger.infrastructure.observability;

import com.polarishb.pabal.messenger.application.port.out.observability.MessageSendMetrics;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class MicrometerMessageSendMetricsAdapter implements MessageSendMetrics {

    private static final String METRIC_NAME = "pabal.messenger.message.send.total";
    private static final String OUTCOME_TAG = "outcome";

    private final Counter sentCounter;
    private final Counter duplicateCounter;
    private final Counter failedCounter;

    public MicrometerMessageSendMetricsAdapter(MeterRegistry meterRegistry) {
        Objects.requireNonNull(meterRegistry, "meterRegistry must not be null");
        this.sentCounter = counter(meterRegistry, "sent");
        this.duplicateCounter = counter(meterRegistry, "duplicate");
        this.failedCounter = counter(meterRegistry, "failed");
    }

    @Override
    public void recordSent() {
        sentCounter.increment();
    }

    @Override
    public void recordDuplicate() {
        duplicateCounter.increment();
    }

    @Override
    public void recordFailed() {
        failedCounter.increment();
    }

    private Counter counter(MeterRegistry meterRegistry, String outcome) {
        return Counter.builder(METRIC_NAME)
                .description("Messenger message send command outcomes")
                .tag(OUTCOME_TAG, outcome)
                .register(meterRegistry);
    }
}
