package com.polarishb.pabal.messenger.application.query.input;

import java.util.UUID;

public class ReadMessageQuery {
    private final UUID messageId;
    private final UUID userId; // User requesting to read the message (for permissions)

    public ReadMessageQuery(UUID messageId, UUID userId) {
        this.messageId = messageId;
        this.userId = userId;
    }

    public UUID getMessageId() {
        return messageId;
    }

    public UUID getUserId() {
        return userId;
    }
}
