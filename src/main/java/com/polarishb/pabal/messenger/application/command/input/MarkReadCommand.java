package com.polarishb.pabal.messenger.application.command.input;

import java.util.UUID;

public record MarkReadCommand(
    UUID chatRoomId,
    UUID userId,
    UUID lastReadMessageId
) {
    public MarkReadCommand(UUID chatRoomId, UUID userId, UUID lastReadMessageId) {
        this.chatRoomId = chatRoomId;
        this.userId = userId;
        this.lastReadMessageId = lastReadMessageId;
    }

    public UUID getChatRoomId() {
        return chatRoomId;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getLastReadMessageId() {
        return lastReadMessageId;
    }
}
