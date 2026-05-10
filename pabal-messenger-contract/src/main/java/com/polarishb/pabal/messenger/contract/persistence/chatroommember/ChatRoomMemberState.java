package com.polarishb.pabal.messenger.contract.persistence.chatroommember;

import com.polarishb.pabal.messenger.domain.model.snapshot.ChatRoomMemberSnapshot;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ChatRoomMemberState(
        ChatRoomMemberSnapshot snapshot,
        Long version
) {
    public ChatRoomMemberState {
        Objects.requireNonNull(snapshot);
    }

    public ChatRoomMemberState(
            UUID id,
            UUID tenantId,
            UUID chatRoomId,
            UUID userId,
            UUID lastReadMessageId,
            Long lastReadSequence,
            Instant lastReadAt,
            Instant joinedAt,
            Instant leftAt,
            Instant createdAt,
            Instant updatedAt,
            Long version
    ) {
        this(
                new ChatRoomMemberSnapshot(
                        id,
                        tenantId,
                        chatRoomId,
                        userId,
                        lastReadMessageId,
                        lastReadSequence,
                        lastReadAt,
                        joinedAt,
                        leftAt,
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

    public UUID userId() {
        return snapshot.userId();
    }

    public UUID lastReadMessageId() {
        return snapshot.lastReadMessageId();
    }

    public Long lastReadSequence() {
        return snapshot.lastReadSequence();
    }

    public Instant lastReadAt() {
        return snapshot.lastReadAt();
    }

    public Instant joinedAt() {
        return snapshot.joinedAt();
    }

    public Instant leftAt() {
        return snapshot.leftAt();
    }

    public Instant createdAt() {
        return snapshot.createdAt();
    }

    public Instant updatedAt() {
        return snapshot.updatedAt();
    }
}
