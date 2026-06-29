package com.polarishb.pabal.workspace.application.command.input;

import com.polarishb.pabal.common.cqrs.Command;

import java.util.Objects;
import java.util.UUID;

public record CreateWorkspaceCommand(
        UUID tenantId,
        UUID ownerId,
        String name
) implements Command {
    public CreateWorkspaceCommand {
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(ownerId, "ownerId must not be null");
    }
}
