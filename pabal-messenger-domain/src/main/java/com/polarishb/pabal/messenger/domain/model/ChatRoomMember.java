package com.polarishb.pabal.messenger.domain.model;

import com.polarishb.pabal.messenger.domain.exception.MemberAlreadyActiveException;
import com.polarishb.pabal.messenger.domain.exception.MemberNotActiveException;
import com.polarishb.pabal.messenger.domain.model.snapshot.ChatRoomMemberSnapshot;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;


@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ChatRoomMember {

    @EqualsAndHashCode.Include
    private final UUID id;
    private final UUID tenantId;
    private final UUID chatRoomId;
    private final UUID userId;

    private final UUID lastReadMessageId;
    private final Long lastReadSequence;
    private final Instant lastReadAt;

    private final Instant joinedAt;
    private final Instant leftAt;

    private final Instant createdAt;
    private final Instant updatedAt;

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

    public static ChatRoomMember reconstitute(ChatRoomMemberSnapshot snapshot) {
        Objects.requireNonNull(snapshot);
        return new ChatRoomMember(
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
                snapshot.updatedAt()
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

    public static ChatRoomMember join(
            UUID tenantId,
            UUID chatRoomId,
            UUID userId,
            Instant joinedAt,
            long initialLastReadSequence
    ) {
        return create(tenantId, chatRoomId, userId, joinedAt, initialLastReadSequence);
    }

    public ChatRoomMember updateLastRead(UUID messageId, long sequence, Instant readAt) {
        if (isStaleLastReadSequence(sequence)) {
            return this;
        }

        return new ChatRoomMember(
                id,
                tenantId,
                chatRoomId,
                userId,
                messageId,
                sequence,
                readAt,
                joinedAt,
                leftAt,
                createdAt,
                readAt
        );
    }

    public boolean wouldAdvanceLastReadCursorTo(long sequence) {
        return this.lastReadSequence == null || sequence > this.lastReadSequence;
    }

    private boolean isStaleLastReadSequence(long sequence) {
        return this.lastReadSequence != null && sequence < this.lastReadSequence;
    }

    public ChatRoomMember leave(Instant leftAt) {
        if (!isActive()) {
            throw new MemberNotActiveException(this.userId);
        }
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
                leftAt
        );
    }

    public ChatRoomMember rejoin(Instant joinedAt, long baselineSequence) {
        if (isActive()) {
            throw new MemberAlreadyActiveException(this.userId);
        }
        return new ChatRoomMember(
                id,
                tenantId,
                chatRoomId,
                userId,
                null,
                baselineSequence,
                null,
                joinedAt,
                null,
                createdAt,
                joinedAt
        );
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
