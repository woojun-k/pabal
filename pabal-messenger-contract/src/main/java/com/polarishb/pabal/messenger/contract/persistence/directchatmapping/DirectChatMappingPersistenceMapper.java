package com.polarishb.pabal.messenger.contract.persistence.directchatmapping;

import com.polarishb.pabal.messenger.domain.model.DirectChatMapping;

public final class DirectChatMappingPersistenceMapper {

    private DirectChatMappingPersistenceMapper() {}

    public static DirectChatMapping toDomain(DirectChatMappingState state) {
        return DirectChatMapping.reconstitute(state.snapshot());
    }

    public static DirectChatMappingState toState(DirectChatMapping mapping, Long version) {
        return new DirectChatMappingState(mapping.snapshot(), version);
    }

    public static PersistedDirectChatMapping toPersisted(DirectChatMappingState state) {
        return new PersistedDirectChatMapping(toDomain(state), state);
    }
}
