package com.polarishb.pabal.messenger.domain.model;

import com.polarishb.pabal.messenger.domain.exception.MessageAlreadyDeletedException;
import com.polarishb.pabal.messenger.domain.model.snapshot.MessageSnapshot;
import com.polarishb.pabal.messenger.domain.model.type.MessageStatus;
import com.polarishb.pabal.messenger.domain.model.type.MessageType;
import com.polarishb.pabal.messenger.domain.model.vo.MessageContent;
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
public class Message {

    public static final UUID SYSTEM_SENDER_ID = new UUID(0L, 0L);

    @EqualsAndHashCode.Include
    private final UUID id;
    private final UUID tenantId;
    private final UUID chatRoomId;
    private final UUID senderId;
    private final UUID clientMessageId;
    private final Long sequence;
    private final MessageType type;
    private final MessageContent content;
    private final MessageStatus status;
    private final UUID replyToMessageId;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final Instant deletedAt;

    public static Message create(
            UUID tenantId,
            UUID chatRoomId,
            UUID senderId,
            UUID clientMessageId,
            String content,
            Instant createdAt
    ) {
        return new Message(
                null,
                tenantId,
                chatRoomId,
                senderId,
                clientMessageId,
                null,
                MessageType.USER,
                new MessageContent(content),
                MessageStatus.ACTIVE,
                null,
                createdAt,
                createdAt,
                null
        );
    }

    public static Message reconstitute(MessageSnapshot snapshot) {
        Objects.requireNonNull(snapshot);
        return new Message(
                snapshot.id(),
                snapshot.tenantId(),
                snapshot.chatRoomId(),
                snapshot.senderId(),
                snapshot.clientMessageId(),
                snapshot.sequence(),
                snapshot.type(),
                snapshot.content(),
                snapshot.status(),
                snapshot.replyToMessageId(),
                snapshot.createdAt(),
                snapshot.updatedAt(),
                snapshot.deletedAt()
        );
    }

    public MessageSnapshot snapshot() {
        return new MessageSnapshot(
                id,
                tenantId,
                chatRoomId,
                senderId,
                clientMessageId,
                sequence,
                type,
                content,
                status,
                replyToMessageId,
                createdAt,
                updatedAt,
                deletedAt
        );
    }

    public static Message createReply(
            UUID tenantId,
            UUID chatRoomId,
            UUID senderId,
            UUID clientMessageId,
            UUID replyToMessageId,
            String content,
            Instant createdAt
    ) {
        return new Message(
                null,
                tenantId,
                chatRoomId,
                senderId,
                clientMessageId,
                null,
                MessageType.USER,
                new MessageContent(content),
                MessageStatus.ACTIVE,
                replyToMessageId,
                createdAt,
                createdAt,
                null
        );
    }

    public Message assignSequence(long sequence) {
        if (this.sequence != null && this.sequence >= sequence) {
            return this;
        }
        return new Message(
                id,
                tenantId,
                chatRoomId,
                senderId,
                clientMessageId,
                sequence,
                type,
                content,
                status,
                replyToMessageId,
                createdAt,
                updatedAt,
                deletedAt
        );
    }

    public Message delete(Instant deletedAt) {
        if (this.status == MessageStatus.DELETED) {
            throw new MessageAlreadyDeletedException();
        }
        return new Message(
                id,
                tenantId,
                chatRoomId,
                senderId,
                clientMessageId,
                sequence,
                type,
                MessageContent.deletedTombstone(),
                MessageStatus.DELETED,
                replyToMessageId,
                createdAt,
                deletedAt,
                deletedAt
        );
    }

    public Message edit(String newContent, Instant updatedAt) {
        if (this.status == MessageStatus.DELETED) {
            throw new MessageAlreadyDeletedException();
        }
        return new Message(
                id,
                tenantId,
                chatRoomId,
                senderId,
                clientMessageId,
                sequence,
                type,
                new MessageContent(newContent),
                MessageStatus.EDITED,
                replyToMessageId,
                createdAt,
                updatedAt,
                deletedAt
        );
    }
}
