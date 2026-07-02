package com.polarishb.pabal.tenant.application.command.input;

import com.polarishb.pabal.common.cqrs.Command;

import java.util.UUID;

public record ActivateTenantRegistrationCommand(
        UUID registrationId
) implements Command {
}
