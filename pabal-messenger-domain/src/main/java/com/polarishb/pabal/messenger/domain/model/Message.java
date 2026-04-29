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
    private UUID id;
    private UUID tenantId;
    private UUID chatRoomId;
    private UUID senderId;
    private UUID clientMessageId;
    private Long sequence;
    private MessageType type;
    private MessageContent content;
    private MessageStatus status;
    private UUID replyToMessageId;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;

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

    public void assignSequence(long sequence) {
        if (this.sequence == null || this.sequence < sequence) {
            this.sequence = sequence;
        }
    }

    public void delete(Instant deletedAt) {
        if (this.status == MessageStatus.DELETED) {
            throw new MessageAlreadyDeletedException();
        }
        this.status = MessageStatus.DELETED;
        this.updatedAt = deletedAt;
        this.deletedAt = deletedAt;
    }

    public void edit(String newContent, Instant updatedAt) {
        if (this.status == MessageStatus.DELETED) {
            throw new MessageAlreadyDeletedException();
        }
        this.content = new MessageContent(newContent);
        this.status = MessageStatus.EDITED;
        this.updatedAt = updatedAt;
    }
}
