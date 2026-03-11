package com.polarishb.pabal.messenger.application.command.input;

import com.polarishb.pabal.common.cqrs.Command;

import java.util.UUID;

public record ScheduleRoomDeletionCommand(
    UUID tenantId,
    UUID roomId,
    UUID requesterId
) implements Command {}
