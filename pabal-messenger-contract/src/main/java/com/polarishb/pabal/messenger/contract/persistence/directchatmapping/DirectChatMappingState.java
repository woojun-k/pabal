package com.polarishb.pabal.messenger.contract.persistence.directchatmapping;

import com.polarishb.pabal.messenger.domain.model.snapshot.DirectChatMappingSnapshot;

import java.time.Instant;
import java.util.UUID;

public record DirectChatMappingState(
        UUID id,
        UUID tenantId,
        UUID chatRoomId,
        UUID userIdMin,
        UUID userIdMax,
        Instant createdAt,
        Instant updatedAt,
        Long version
) {
    public DirectChatMappingState(
            DirectChatMappingSnapshot snapshot,
            Long version
    ) {
        this(
                snapshot.id(),
                snapshot.tenantId(),
                snapshot.chatRoomId(),
                snapshot.userIdMin(),
                snapshot.userIdMax(),
                snapshot.createdAt(),
                snapshot.updatedAt(),
                version
        );
    }

    public DirectChatMappingSnapshot snapshot() {
        return new DirectChatMappingSnapshot(
                id,
                tenantId,
                chatRoomId,
                userIdMin,
                userIdMax,
                createdAt,
                updatedAt
        );
    }
}
