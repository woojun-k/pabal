package com.polarishb.pabal.messenger.application.command.input;

import com.polarishb.pabal.messenger.application.command.SendableCommand;

import java.util.UUID;

public record SendReplyCommand(
    UUID tenantId,
    UUID senderId,
    UUID chatRoomId,
    UUID clientMessageId,
    UUID replyToMessageId,
    String content
) implements SendableCommand {}
