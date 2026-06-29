package com.polarishb.pabal.tenant.application.command.input;

import com.polarishb.pabal.common.cqrs.Command;

public record RequestTenantRegistrationCommand(
        String tenantName,
        String domainName
) implements Command {
}
