package com.polarishb.pabal.messenger.domain.model.entity;

import com.polarishb.pabal.common.persistence.jpa.UuidV7Generated;
import com.polarishb.pabal.messenger.domain.model.type.MessageStatus;
import com.polarishb.pabal.messenger.domain.model.type.MessageType;
import com.polarishb.pabal.messenger.domain.model.vo.MessageContent;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "message",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"chat_room_id", "sender_id", "client_message_id"})
    },
    indexes = {
        @Index(name = "idx_chat_room_created", columnList = "chatRoomId,createdAt,uuid")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Message {

    @Id
    @UuidV7Generated(mode = UuidV7Generated.Mode.MONOTONIC)
    private UUID uuid;

    @Column(nullable = false)
    private UUID chatRoomId;

    @Column(nullable = false)
    private UUID senderId;

    @Column(nullable = false)
    private UUID clientMessageId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageType type;

    @Embedded
    private MessageContent content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageStatus status;

    private UUID replyToMessageId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant updatedAt;

    private Instant deletedAt;

    @Builder
    private Message(
            UUID chatRoomId,
            UUID senderId,
            UUID clientMessageId,
            MessageType type,
            MessageContent content,
            UUID replyToMessageId
    ) {
        this.chatRoomId = chatRoomId;
        this.senderId = senderId;
        this.clientMessageId = clientMessageId;
        this.type = type;
        this.content = content;
        this.status = MessageStatus.ACTIVE;
        this.replyToMessageId = replyToMessageId;
        this.createdAt = Instant.now();
    }

    public static Message create(UUID chatRoomId, UUID senderId, UUID clientMessageId, MessageType type, String content) {
        return Message.builder()
                .chatRoomId(chatRoomId)
                .senderId(senderId)
                .clientMessageId(clientMessageId)
                .type(type)
                .content(new MessageContent(content))
                .build();
    }

    public static Message createReply(
            UUID chatRoomId,
            UUID senderId,
            UUID clientMessageId,
            String content,
            UUID replyToMessageId
    ) {
        return Message.builder()
                .chatRoomId(chatRoomId)
                .senderId(senderId)
                .clientMessageId(clientMessageId)
                .content(new MessageContent(content))
                .replyToMessageId(replyToMessageId)
                .build();
    }

    public void delete() {
        if (this.status == MessageStatus.DELETED) {
            throw new IllegalStateException("이미 삭제된 메시지입니다");
        }
        this.status = MessageStatus.DELETED;
        this.updatedAt = Instant.now();
        this.deletedAt = Instant.now();
    }

    public void edit(String newContent) {
        if (this.status == MessageStatus.DELETED) {
            throw new IllegalStateException("삭제된 메시지는 수정할 수 없습니다");
        }
        this.content = new MessageContent(newContent);
        this.status = MessageStatus.EDITED;
        this.updatedAt = Instant.now();
    }
}