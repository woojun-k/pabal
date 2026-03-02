package com.polarishb.pabal.messenger.application.command.input;

import com.polarishb.pabal.common.cqrs.Command;

import java.util.UUID;

public record GetOrCreateDirectRoomCommand(
    UUID tenantId,
    UUID requesterId,
    UUID participantId
) implements Command {}
