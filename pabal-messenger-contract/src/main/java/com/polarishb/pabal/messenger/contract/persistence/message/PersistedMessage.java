package com.polarishb.pabal.messenger.contract.persistence.message;

import com.polarishb.pabal.messenger.domain.model.Message;

import java.util.Objects;

public record PersistedMessage(
    Message message,
    MessageState state
) {
    public PersistedMessage {
        Objects.requireNonNull(message);
        Objects.requireNonNull(state);
        if (!Objects.equals(message.getTenantId(), state.tenantId())) {
            throw new IllegalArgumentException("message tenantId must match persisted state tenantId");
        }
    }
}
