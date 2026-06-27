package com.polarishb.pabal.tenant.application.command.input;

import com.polarishb.pabal.common.cqrs.Command;

import java.util.UUID;

public record RenewTenantRegistrationTokenCommand(
        UUID registrationId
) implements Command {
}
