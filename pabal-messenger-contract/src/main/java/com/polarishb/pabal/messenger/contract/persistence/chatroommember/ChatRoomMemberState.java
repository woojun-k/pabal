package com.polarishb.pabal.messenger.contract.persistence.chatroommember;

import com.polarishb.pabal.messenger.domain.model.snapshot.ChatRoomMemberSnapshot;

import java.time.Instant;
import java.util.UUID;

public record ChatRoomMemberState(
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
    public ChatRoomMemberState(
            ChatRoomMemberSnapshot snapshot,
            Long version
    ) {
        this(
                snapshot.id(),
                snapshot.tenantId(),
                snapshot.chatRoomId(),
                snapshot.userId(),
                snapshot.lastReadMessageId(),
                snapshot.lastReadSequence(),
                snapshot.lastReadAt(),
                snapshot.joinedAt(),
                snapshot.leftAt(),
                snapshot.createdAt(),
                snapshot.updatedAt(),
                version
        );
    }

    public ChatRoomMemberSnapshot snapshot() {
        return new ChatRoomMemberSnapshot(
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
        );
    }
}
