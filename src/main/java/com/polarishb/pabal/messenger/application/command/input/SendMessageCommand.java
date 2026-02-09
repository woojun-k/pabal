package com.polarishb.pabal.messenger.application.command.input;

import com.polarishb.pabal.common.cqrs.Command;

import java.util.UUID;

public record SendMessageCommand (
    UUID tenantId,
    UUID senderId,
    UUID chatRoomId,
    UUID clientMessageId,
    UUID replyToMessageId,
    String content
) implements Command { }
