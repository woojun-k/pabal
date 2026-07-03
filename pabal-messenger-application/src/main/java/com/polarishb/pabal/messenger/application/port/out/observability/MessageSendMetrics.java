package com.polarishb.pabal.messenger.application.port.out.observability;

public interface MessageSendMetrics {

    void recordSent();

    void recordDuplicate();

    void recordFailed();
}
