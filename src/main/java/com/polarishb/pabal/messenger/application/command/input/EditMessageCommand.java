package com.polarishb.pabal.messenger.application.command.input;

import java.util.UUID;

public record EditMessageCommand(
    UUID messageId,
    String newContent,
    UUID requestorId
) {
    // User requesting the edit
    public EditMessageCommand(UUID messageId, String newContent, UUID requestorId) {
        this.messageId = messageId;
        this.newContent = newContent;
        this.requestorId = requestorId;
    }

    public UUID getMessageId() {
        return messageId;
    }

    public String getNewContent() {
        return newContent;
    }

    public UUID getRequestorId() {
        return requestorId;
    }
}
