package com.polarishb.pabal.user.application.command.input;

import com.polarishb.pabal.common.cqrs.Command;

import java.util.UUID;

public record CreateUserCommand(
        UUID userId,
        UUID tenantId,
        String name
) implements Command {
}
