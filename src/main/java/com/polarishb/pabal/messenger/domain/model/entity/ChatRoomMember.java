package com.polarishb.pabal.messenger.domain.model.entity;

import com.polarishb.pabal.messenger.domain.exception.MemberAlreadyActiveException;
import com.polarishb.pabal.messenger.domain.exception.MemberNotActiveException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;


@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ChatRoomMember {

    @EqualsAndHashCode.Include
    private UUID id;
    private UUID tenantId;
    private UUID chatRoomId;
    private UUID userId;

    private UUID lastReadMessageId;
    private Instant lastReadAt;

    private Instant joinedAt;
    private Instant leftAt;

    private Instant createdAt;
    private Instant updatedAt;

    public static ChatRoomMember create(
        UUID tenantId,
        UUID chatRoomId,
        UUID userId,
        Instant joinedAt
    ) {
        return new ChatRoomMember(
            null,
            tenantId,
            chatRoomId,
            userId,
            null,
            null,
            joinedAt,
            null,
            joinedAt, // createdAt
            joinedAt  // updatedAt
        );
    }

    public static ChatRoomMember reconstitute(
         UUID id,
         UUID tenantId,
         UUID chatRoomId,
         UUID userId,
         UUID lastReadMessageId,
         Instant lastReadAt,
         Instant joinedAt,
         Instant leftAt,
         Instant createdAt,
         Instant updatedAt
    ) {
        return new ChatRoomMember(
            id,
            tenantId,
            chatRoomId,
            userId,
            lastReadMessageId,
            lastReadAt,
            joinedAt,
            leftAt,
            createdAt,
            updatedAt
        );
    }

    public static ChatRoomMember join(UUID tenantId, UUID chatRoomId, UUID userId, Instant joinedAt) {
        return create(tenantId, chatRoomId, userId, joinedAt);
    }

    public void updateLastRead(UUID messageId, Instant readAt) {
        this.lastReadMessageId = messageId;
        this.lastReadAt = readAt;
        this.updatedAt = readAt;
    }

    public void leave(Instant leftAt) {
        if (!isActive()) {
            throw new MemberNotActiveException(this.userId);
        }
        this.leftAt = leftAt;
        this.updatedAt = leftAt;
    }

    public void rejoin(Instant joinedAt) {
        if (isActive()) {
            throw new MemberAlreadyActiveException(this.userId);
        }
        this.leftAt = null;
        this.joinedAt = joinedAt;
        this.updatedAt = joinedAt;
    }

    public boolean isActive() {
        return this.leftAt == null;
    }
}
