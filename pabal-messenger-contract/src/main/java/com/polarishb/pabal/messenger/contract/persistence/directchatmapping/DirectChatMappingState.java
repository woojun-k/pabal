package com.polarishb.pabal.messenger.contract.persistence.directchatmapping;

import com.polarishb.pabal.messenger.domain.model.snapshot.DirectChatMappingSnapshot;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record DirectChatMappingState(
        DirectChatMappingSnapshot snapshot,
        Long version
) {
    public DirectChatMappingState {
        Objects.requireNonNull(snapshot);
    }

    public DirectChatMappingState(
            UUID id,
            UUID tenantId,
            UUID chatRoomId,
            UUID userIdMin,
            UUID userIdMax,
            Instant createdAt,
            Instant updatedAt,
            Long version
    ) {
        this(
                new DirectChatMappingSnapshot(
                        id,
                        tenantId,
                        chatRoomId,
                        userIdMin,
                        userIdMax,
                        createdAt,
                        updatedAt
                ),
                version
        );
    }

    public UUID id() {
        return snapshot.id();
    }

    public UUID tenantId() {
        return snapshot.tenantId();
    }

    public UUID chatRoomId() {
        return snapshot.chatRoomId();
    }

    public UUID userIdMin() {
        return snapshot.userIdMin();
    }

    public UUID userIdMax() {
        return snapshot.userIdMax();
    }

    public Instant createdAt() {
        return snapshot.createdAt();
    }

    public Instant updatedAt() {
        return snapshot.updatedAt();
    }
}
