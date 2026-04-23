package com.polarishb.pabal.messenger.application.command.input;

import com.polarishb.pabal.messenger.application.command.SendableCommand;

import java.util.Objects;
import java.util.UUID;

public record SendMessageCommand (
    UUID tenantId,
    UUID senderId,
    UUID chatRoomId,
    UUID clientMessageId,
    String content
) implements SendableCommand {
    public SendMessageCommand {
        Objects.requireNonNull(tenantId);
        Objects.requireNonNull(senderId);
        Objects.requireNonNull(chatRoomId);
        Objects.requireNonNull(clientMessageId);
        Objects.requireNonNull(content);
    }
}
