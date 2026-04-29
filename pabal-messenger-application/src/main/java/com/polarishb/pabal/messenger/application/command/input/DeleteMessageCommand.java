package com.polarishb.pabal.messenger.application.command.input;

import com.polarishb.pabal.common.cqrs.Command;

import java.util.UUID;

public record DeleteMessageCommand(
    UUID tenantId,
    UUID messageId,
    UUID requesterId
) implements Command {}
