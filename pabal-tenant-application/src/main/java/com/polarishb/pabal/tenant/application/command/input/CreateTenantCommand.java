package com.polarishb.pabal.tenant.application.command.input;

import com.polarishb.pabal.common.cqrs.Command;

public record CreateTenantCommand(
        String name
) implements Command {
}
