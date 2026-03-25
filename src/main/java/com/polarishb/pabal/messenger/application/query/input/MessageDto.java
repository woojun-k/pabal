package com.polarishb.pabal.messenger.application.query.input;

import com.polarishb.pabal.messenger.domain.model.type.MessageStatus;
import com.polarishb.pabal.messenger.domain.model.vo.MessageContent;
import java.time.Instant;
import java.util.UUID;

public class MessageDto {
    private final UUID id;
    private final UUID chatRoomId;
    private final UUID senderId;
    private final MessageContent content; // Assuming MessageContent is a Value Object
    private final Instant createdAt;
    private final Instant updatedAt;
    private final MessageStatus status;

    public MessageDto(UUID id, UUID chatRoomId, UUID senderId, MessageContent content, Instant createdAt, Instant updatedAt, MessageStatus status) {
        this.id = id;
        this.chatRoomId = chatRoomId;
        this.senderId = senderId;
        this.content = content;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.status = status;
    }

    public UUID getId() {
        return id;
    }

    public UUID getChatRoomId() {
        return chatRoomId;
    }

    public UUID getSenderId() {
        return senderId;
    }

    public MessageContent getContent() {
        return content;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public MessageStatus getStatus() {
        return status;
    }
}
