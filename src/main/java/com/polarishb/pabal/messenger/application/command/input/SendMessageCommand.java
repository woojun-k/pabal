package com.polarishb.pabal.messenger.application.command.input;

import java.util.UUID;

public record SendMessageCommand(
    UUID tenantId,
    UUID senderId,
    UUID chatRoomId,
    UUID clientMessageId,
    String content
) {
    // TODO: Add fields for sender, chat room, message content, etc.
}
