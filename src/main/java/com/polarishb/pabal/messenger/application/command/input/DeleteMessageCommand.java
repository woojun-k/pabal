package com.polarishb.pabal.messenger.application.command.input;

import java.util.UUID;

public record DeleteMessageCommand(
    UUID messageId,
    UUID requestorId // User requesting the deletion
) {
    public DeleteMessageCommand(UUID messageId, UUID requestorId) {
        this.messageId = messageId;
        this.requestorId = requestorId;
    }

    public UUID getMessageId() {
        return messageId;
    }

    public UUID getRequestorId() {
        return requestorId;
    }
}
