package com.polarishb.pabal.messenger.domain.model.entity;

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
    private UUID chatRoomId;
    private UUID userId;

    private UUID lastReadMessageId;
    private Instant lastReadAt;

    private Instant joinedAt;
    private Instant leftAt;

    public static ChatRoomMember create(
        UUID chatRoomId,
        UUID userId,
        Instant joinedAt
    ) {
        return new ChatRoomMember(
            null,
            chatRoomId,
            userId,
            null,
            null,
            joinedAt,
            null
        );
    }

    public static ChatRoomMember reconstitute(
         UUID id,
         UUID chatRoomId,
         UUID userId,
         UUID lastReadMessageId,
         Instant lastReadAt,
         Instant joinedAt,
         Instant leftAt
    ) {
        return new ChatRoomMember(
            id,
            chatRoomId,
            userId,
            lastReadMessageId,
            lastReadAt,
            joinedAt,
            leftAt
        );
    }

    public static ChatRoomMember join(UUID chatRoomId, UUID userId, Instant joinedAt) {
        return create(chatRoomId, userId, joinedAt);
    }

    public void updateLastRead(UUID messageId, Instant readAt) {
        this.lastReadMessageId = messageId;
        this.lastReadAt = readAt;
    }

    public void leave(Instant leftAt) {
        this.leftAt = leftAt;
    }

    public void rejoin(Instant joinedAt) {
        this.leftAt = null;
        this.joinedAt = joinedAt;
    }

    public boolean isActive() {
        return this.leftAt == null;
    }
}
