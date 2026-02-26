package com.polarishb.pabal.messenger.domain.model.entity;

import com.polarishb.pabal.messenger.domain.model.type.MessageStatus;
import com.polarishb.pabal.messenger.domain.model.type.MessageType;
import com.polarishb.pabal.messenger.domain.model.vo.MessageContent;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Message {

    public static final UUID SYSTEM_SENDER_ID = new UUID(0L, 0L);

    @EqualsAndHashCode.Include
    private UUID id;
    private UUID chatRoomId;
    private UUID senderId;
    private UUID clientMessageId;
    private MessageType type;
    private MessageContent content;
    private MessageStatus status;
    private UUID replyToMessageId;
    private Instant createdAt;

    public static Message create(
            UUID chatRoomId,
            UUID senderId,
            UUID clientMessageId,
            String content,
            Instant createdAt
    ) {
        return new Message(
                null,
                chatRoomId,
                senderId,
                clientMessageId,
                MessageType.USER,
                new MessageContent(content),
                MessageStatus.ACTIVE,
                null,
                createdAt
        );
    }

    public static Message reconstitute(
            UUID id,
            UUID chatRoomId,
            UUID senderId,
            UUID clientMessageId,
            MessageType type,
            MessageContent content,
            MessageStatus status,
            UUID replyToMessageId,
            Instant createdAt
    ) {
        return new Message(
                id,
                chatRoomId,
                senderId,
                clientMessageId,
                type,
                content,
                status,
                replyToMessageId,
                createdAt
        );
    }

    public static Message createReply(
            UUID chatRoomId,
            UUID senderId,
            UUID clientMessageId,
            UUID replyToMessageId,
            String content,
            Instant createdAt
    ) {
        return new Message(
                null,
                chatRoomId,
                senderId,
                clientMessageId,
                MessageType.USER,
                new MessageContent(content),
                MessageStatus.ACTIVE,
                replyToMessageId,
                createdAt
        );
    }

    public void delete() {
        if (this.status == MessageStatus.DELETED) {
            throw new IllegalStateException("이미 삭제된 메시지입니다");
        }
        this.status = MessageStatus.DELETED;
    }

    public void edit(String newContent) {
        if (this.status == MessageStatus.DELETED) {
            throw new IllegalStateException("삭제된 메시지는 수정할 수 없습니다");
        }
        this.content = new MessageContent(newContent);
        this.status = MessageStatus.EDITED;
    }
}