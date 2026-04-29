package com.polarishb.pabal.messenger.application.command.input;

import com.polarishb.pabal.common.cqrs.Command;

import java.util.UUID;

public record MarkReadCommand(
    UUID tenantId,
    UUID chatRoomId,
    UUID userId,
    UUID lastReadMessageId
) implements Command {}
