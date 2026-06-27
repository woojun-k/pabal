package com.polarishb.pabal.tenant.application.query.input;

import com.polarishb.pabal.common.cqrs.Query;

import java.util.UUID;

public record GetTenantRegistrationQuery(
        UUID registrationId
) implements Query {
}
