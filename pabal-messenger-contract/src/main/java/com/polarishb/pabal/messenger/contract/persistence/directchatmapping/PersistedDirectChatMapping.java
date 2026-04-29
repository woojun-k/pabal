package com.polarishb.pabal.messenger.contract.persistence.directchatmapping;

import com.polarishb.pabal.messenger.domain.model.DirectChatMapping;

import java.util.Objects;

public record PersistedDirectChatMapping(
    DirectChatMapping mapping,
    DirectChatMappingState state
) {
    public PersistedDirectChatMapping {
        Objects.requireNonNull(mapping);
        Objects.requireNonNull(state);
    }
}
