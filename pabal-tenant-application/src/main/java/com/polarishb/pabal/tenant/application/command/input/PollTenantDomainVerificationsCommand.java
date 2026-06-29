package com.polarishb.pabal.tenant.application.command.input;

import com.polarishb.pabal.common.cqrs.Command;

public record PollTenantDomainVerificationsCommand(
        int limit
) implements Command {
    public PollTenantDomainVerificationsCommand {
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be positive");
        }
    }
}
