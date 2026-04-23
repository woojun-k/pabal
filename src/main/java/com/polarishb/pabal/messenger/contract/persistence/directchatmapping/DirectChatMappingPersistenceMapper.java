package com.polarishb.pabal.messenger.contract.persistence.directchatmapping;

import com.polarishb.pabal.messenger.domain.model.entity.DirectChatMapping;

import java.time.Instant;

public final class DirectChatMappingPersistenceMapper {

    private DirectChatMappingPersistenceMapper() {}

    public static DirectChatMapping toDomain(DirectChatMappingState state) {
        return DirectChatMapping.reconstitute(
                state.id(),
                state.tenantId(),
                state.chatRoomId(),
                state.userIdMin(),
                state.userIdMax(),
                state.createdAt(),
                state.updatedAt()
        );
    }

    public static DirectChatMappingState toState(DirectChatMapping mapping, Long version) {
        return new DirectChatMappingState(
                mapping.getId(),
                mapping.getTenantId(),
                mapping.getChatRoomId(),
                mapping.getUserIdMin(),
                mapping.getUserIdMax(),
                mapping.getCreatedAt(),
                mapping.getUpdatedAt(),
                version
        );
    }

    public static PersistedDirectChatMapping toPersisted(DirectChatMappingState state) {
        return new PersistedDirectChatMapping(toDomain(state), state);
    }
}
