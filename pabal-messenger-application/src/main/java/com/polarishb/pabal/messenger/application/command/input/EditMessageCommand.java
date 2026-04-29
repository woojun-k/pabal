package com.polarishb.pabal.messenger.application.command.input;

import com.polarishb.pabal.common.cqrs.Command;

import java.util.UUID;

public record EditMessageCommand(
    UUID tenantId,
    UUID messageId,
    UUID requesterId,
    String newContent
) implements Command {}
