package com.polarishb.pabal.messenger.application.command.input;

import com.polarishb.pabal.common.cqrs.Command;

import java.util.UUID;

public record DeleteRoomImmediatelyCommand(
    UUID tenantId,
    UUID chatRoomId,
    UUID requesterId
) implements Command { }
