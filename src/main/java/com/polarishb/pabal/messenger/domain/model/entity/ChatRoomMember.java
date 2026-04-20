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
    private Long lastReadSequence;
    private Instant lastReadAt;

    private Instant joinedAt;
    private Instant leftAt;

    private Instant createdAt;
    private Instant updatedAt;

    public static ChatRoomMember create(
        UUID tenantId,
        UUID chatRoomId,
        UUID userId,
        Instant joinedAt,
        long initialLastReadSequence
    ) {
        return new ChatRoomMember(
            null,
            tenantId,
            chatRoomId,
            userId,
            null,
            initialLastReadSequence,
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
         Long lastReadSequence,
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
            lastReadSequence,
            lastReadAt,
            joinedAt,
            leftAt,
            createdAt,
            updatedAt
        );
    }

    public static ChatRoomMember join(
            UUID tenantId,
            UUID chatRoomId,
            UUID userId,
            Instant joinedAt,
            long initialLastReadSequence
    ) {
        return create(tenantId, chatRoomId, userId, joinedAt, initialLastReadSequence);
    }

    public void updateLastRead(UUID messageId, long sequence, Instant readAt) {
        if (this.lastReadSequence != null && sequence < this.lastReadSequence) {
            return;
        }

        this.lastReadMessageId = messageId;
        this.lastReadSequence = sequence;
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

    public void rejoin(Instant joinedAt, long baselineSequence) {
        if (isActive()) {
            throw new MemberAlreadyActiveException(this.userId);
        }
        this.leftAt = null;
        this.joinedAt = joinedAt;
        this.lastReadMessageId = null;
        this.lastReadSequence = baselineSequence;
        this.lastReadAt = null;
        this.updatedAt = joinedAt;
    }

    public void validateActive() {
        if (!isActive()) {
            throw new MemberNotActiveException(this.userId);
        }
    }

    public boolean isActive() {
        return this.leftAt == null;
    }
}
