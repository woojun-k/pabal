package com.polarishb.pabal.messenger.infrastructure.observability;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MicrometerMessageSendMetricsAdapterTest {

    private static final String METRIC_NAME = "pabal.messenger.message.send.total";

    @Test
    void records_message_send_outcomes_with_low_cardinality_tags() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        MicrometerMessageSendMetricsAdapter adapter = new MicrometerMessageSendMetricsAdapter(meterRegistry);

        adapter.recordSent();
        adapter.recordDuplicate();
        adapter.recordFailed();

        assertThat(counter(meterRegistry, "sent")).isEqualTo(1.0);
        assertThat(counter(meterRegistry, "duplicate")).isEqualTo(1.0);
        assertThat(counter(meterRegistry, "failed")).isEqualTo(1.0);
    }

    private double counter(SimpleMeterRegistry meterRegistry, String outcome) {
        return meterRegistry.get(METRIC_NAME)
                .tag("outcome", outcome)
                .counter()
                .count();
    }
}
